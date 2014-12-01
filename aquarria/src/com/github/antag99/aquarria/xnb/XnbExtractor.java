/*******************************************************************************
 * Copyright (c) 2014, Anton Gustafsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * * Neither the name of Aquarria nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.github.antag99.aquarria.xnb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;

public abstract class XnbExtractor {
	private static final int HEADER_SIZE = 14;

	private LzxDecoder lzxDecoder = new LzxDecoder();

	protected ByteBuffer buffer;
	protected byte targetPlatform;
	protected int xnaVersion;
	protected boolean wasCompressed;
	protected int decompressedSize;
	protected int compressedSize;
	protected String primaryTypeReaderName;
	protected String primaryType;
	protected int primaryTypeReaderVersion;

	public void extract(FileHandle source, FileHandle dest) {
		readHeader(source);
	}

	protected void readHeader(FileHandle file) {
		buffer = ByteBuffer.wrap(file.readBytes());
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		if (buffer.get() != 'X' || buffer.get() != 'N' || buffer.get() != 'B') {
			throw new RuntimeException("Invalid XNB header");
		}

		targetPlatform = buffer.get();
		xnaVersion = buffer.get();
		wasCompressed = (buffer.get() & 0x80) != 0;

		compressedSize = buffer.getInt();
		decompressedSize = wasCompressed ? buffer.getInt() : compressedSize;

		// XNB files can be compressed with a slightly modified LZX algorithm
		if (wasCompressed) {
			ByteBuffer decompressedBuffer = ByteBuffer.allocate(decompressedSize);
			decompressedBuffer.order(ByteOrder.LITTLE_ENDIAN);

			lzxDecoder.decompress(buffer, compressedSize - HEADER_SIZE, decompressedBuffer, decompressedSize);

			decompressedBuffer.position(0);

			buffer = decompressedBuffer;
		}

		int typeReaderCount = get7BitEncodedInt(buffer);

		// The first type reader is used for reading the primary asset
		primaryTypeReaderName = getCSharpString(buffer);
		primaryTypeReaderVersion = buffer.getInt();

		// Type reader names MIGHT contain assembly information
		int assemblyInformationIndex = primaryTypeReaderName.indexOf(',');
		if (assemblyInformationIndex != -1) {
			primaryTypeReaderName = primaryTypeReaderName.substring(0, assemblyInformationIndex);
		}

		primaryType = primaryTypeReaderName.replace("Microsoft.Xna.Framework.Content.", "").replace("Reader", "");

		// Skip the remaining type readers, as all types are known
		for (int k = 1; k < typeReaderCount; ++k) {
			getCSharpString(buffer);
			buffer.getInt();
		}

		if (get7BitEncodedInt(buffer) != 0) {
			throw new RuntimeException("Shared resources are not supported");
		}

		if (get7BitEncodedInt(buffer) != 1) {
			throw new RuntimeException("Primary asset is invalid");
		}
	}

	protected static int get7BitEncodedInt(ByteBuffer buffer) {
		int result = 0;
		int bitsRead = 0;
		int value;

		do {
			value = buffer.get();
			result |= (value & 0x7f) << bitsRead;
			bitsRead += 7;
		} while ((value & 0x80) != 0);

		return result;
	}

	protected static char getCSharpChar(ByteBuffer buffer) {
		char result = (char) buffer.get();
		if ((result & 0x80) != 0) {
			int bytes = 1;
			while ((result & (0x80 >> bytes)) != 0)
				bytes++;
			result &= (1 << (8 - bytes)) - 1;
			while (--bytes > 0) {
				result <<= 6;
				result |= buffer.get() & 0x3F;
			}
		}
		return result;
	}

	protected static <T> void getList(ByteBuffer buffer, List<T> list, Class<T> clazz) {
		if (get7BitEncodedInt(buffer) == 0) {
			throw new RuntimeException("List is null");
		}

		int len = buffer.getInt();
		for (int i = 0; i < len; ++i) {
			if (clazz == Rectangle.class) {
				list.add(clazz.cast(getRectangle(buffer)));
			} else if (clazz == Vector3.class) {
				list.add(clazz.cast(getVector3(buffer)));
			} else {
				throw new RuntimeException("Unsupported array type");
			}
		}
	}

	private static Charset utf8 = Charset.forName("UTF-8");

	protected static String getCSharpString(ByteBuffer buffer) {
		int len = get7BitEncodedInt(buffer);
		byte[] buf = new byte[len];
		buffer.get(buf);

		return new String(buf, utf8);
	}

	protected static Rectangle getRectangle(ByteBuffer buffer) {
		return new Rectangle(buffer.getInt(), buffer.getInt(), buffer.getInt(), buffer.getInt());
	}

	protected static Vector3 getVector3(ByteBuffer buffer) {
		return new Vector3(buffer.getFloat(), buffer.getFloat(), buffer.getFloat());
	}
}
