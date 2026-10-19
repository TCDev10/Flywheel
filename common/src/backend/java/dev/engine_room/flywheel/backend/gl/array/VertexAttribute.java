package dev.engine_room.flywheel.backend.gl.array;

public sealed interface VertexAttribute {
	int byteWidth();

	public enum Type {
		BYTE(1, 0x1400),
		UBYTE(1, 0x1401),
		SHORT(2, 0x1402),
		USHORT(2, 0x1403),
		INT(4, 0x1404),
		UINT(4, 0x1405),
		FLOAT(4, 0x1406);

		private final int size;
		private final int glType;

		Type(int size, int glType) {
			this.size = size;
			this.glType = glType;
		}

		public int size() {
			return size;
		}

		public int glType() {
			return glType;
		}
	}

	record Float(Type type, int size, boolean normalized) implements VertexAttribute {
		@Override
		public int byteWidth() {
			return size * type.size();
		}
	}

	record Int(Type type, int size) implements VertexAttribute {
		@Override
		public int byteWidth() {
			return size * type.size();
		}
	}
}
