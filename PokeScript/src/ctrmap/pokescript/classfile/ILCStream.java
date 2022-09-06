package ctrmap.pokescript.classfile;

import xstandard.io.base.iface.IOStream;
import xstandard.io.base.impl.ext.data.DataIOStream;
import xstandard.io.structs.StringTable;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Stack;

public class ILCStream extends DataIOStream {

	private StringTable stringTable;

	public static final int ILC_BOM = 0xBE1E;
	public static final int ILC_BOM_BE = 0xBE1E;
	public static final int ILC_BOM_LE = 0x1EBE;

	private int fileVersion = ILCVersion.CURRENT;

	private Stack<Integer> parentFieldOffsets = new Stack<>();
	private Stack<ILCFieldID> parentFieldIds = new Stack<>();
	
	private int currentFieldOffs = -1;
	private ILCFieldID currentFieldId = null;

	public ILCStream(IOStream io) {
		super(io);
		stringTable = new StringTable(this);
	}

	@Override
	public void writeString(String str) {
		stringTable.putStringOffset(str);
	}

	public void writeILCObjHeader(ILCObjTag tag) throws IOException {
		writeStringUnterminated(tag.fourcc);
		writeShort(ILC_BOM);
		writeShort(0);
		writeInt(ILCVersion.CURRENT);
	}
	
	public <O, FI extends ILCFieldID> O readObject(O dest, Class<FI> fieldIdClass, ILCObjectReaderCallback<O, FI> fieldReader) throws IOException {
		int fieldId;
		int fieldStart = getPositionUnbased();
		int fieldEnd;
		int fieldSize;

		FI[] enumConstants = fieldIdClass.getEnumConstants();

		while (true) {
			fieldId = readUnsignedShort();
			fieldSize = readUnsignedShort();
			
			if (fieldId == 0xFFFF) {
				break;
			}
			//System.out.println("FLD " + fieldIdClass + " id " + fieldId + " pos " + Integer.toHexString(fieldStart) + " fsize " + (fieldSize << 2));

			fieldReader.read(this, dest, enumConstants[fieldId]);

			fieldEnd = fieldStart + (fieldSize << 2);
			seek(fieldEnd);
			fieldStart = fieldEnd;
		}
		
		return dest;
	}
	
	public <T> T[] readArray(Class<T> elemClass, ILCListReaderCallback<T> callback) throws IOException {
		Object arrayObj = Array.newInstance(elemClass, readInt());
		
		int elemIndex;
		int fieldStart = getPositionUnbased();
		int fieldEnd;
		int fieldSize;

		while (true) {
			elemIndex = readUnsignedShort();
			fieldSize = readUnsignedShort();
			
			if (elemIndex == 0xFFFF) {
				break;
			}

			Array.set(arrayObj, elemIndex, callback.read(this));

			fieldEnd = fieldStart + (fieldSize << 2);
			seek(fieldEnd);
			fieldStart = fieldEnd;
		}
		
		return (T[]) arrayObj;
	}
	
	public void writeIntArray(ILCFieldID id, int[] array) throws IOException {
		beginWriteField(id);
		
		writeInt(array.length);
		for (int i : array) {
			writeInt(i);
		}
		
		endWriteField();
	}
	
	public void writeFloatArray(ILCFieldID id, float[] array) throws IOException {
		beginWriteField(id);
		
		writeInt(array.length);
		for (float f : array) {
			writeFloat(f);
		}
		
		endWriteField();
	}
	
	public void writeStringList(ILCFieldID id, List<String> array) throws IOException {
		beginWriteField(id);
		
		writeInt(array.size());
		for (String str : array) {
			writeString(str);
		}
		
		endWriteField();
	}

	public <T> void writeArray(ILCFieldID id, T[] value, ILCListWriterCallback<T> callback) throws IOException {
		beginWriteField(id);

		writeInt(value.length);

		for (int i = 0; i < value.length; i++) {
			beginWriteListElem(i);
			callback.write(this, value[i]);
			endWriteField();
		}

		endObject();
		
		endWriteField();
	}

	public <T> void writeList(ILCFieldID id, List<T> value, ILCListWriterCallback<T> callback) throws IOException {
		beginWriteField(id);

		writeInt(value.size());

		int index = 0;
		for (T elem : value) {
			beginWriteListElem(index);
			callback.write(this, elem);
			endWriteField();
			index++;
		}

		endWriteField();
	}
	
	public static interface ILCObjectReaderCallback<O, FI> {

		public void read(ILCStream in, O obj, FI fieldId) throws IOException;
	}

	public static interface ILCObjectFieldReaderCallback<FI> {

		public void read(ILCStream in, FI fieldId) throws IOException;
	}

	public static interface ILCListWriterCallback<T> {

		public void write(ILCStream out, T elem) throws IOException;
	}
	
	public static interface ILCListReaderCallback<T> {
		public T read(ILCStream in) throws IOException;
	}

	public void writeStringILC(ILCFieldID id, String value) throws IOException {
		if (value != null) {
			beginWriteField(id);
			writeString(value);
			endWriteField();
		}
	}

	public void writeIntILC(ILCFieldID id, int value) throws IOException {
		beginWriteField(id);
		writeInt(value);
		endWriteField();
	}

	public void writeEnum32ILC(ILCFieldID id, Enum value) throws IOException {
		beginWriteField(id);
		writeInt(value.ordinal());
		endWriteField();
	}
	
	public <T extends Enum> T readEnum32ILC(Class<T> cls) throws IOException {
		return cls.getEnumConstants()[readInt()];
	}

	public void beginWriteListElem(int index) throws IOException {
		if (currentFieldOffs != -1) {
			parentFieldOffsets.push(currentFieldOffs);
			parentFieldIds.push(currentFieldId);
		}
		currentFieldOffs = getPositionUnbased();
		writeShort(index);
		writeShort(0);
	}

	public void beginWriteField(ILCFieldID fieldId) throws IOException {
		if (currentFieldOffs != -1) {
			parentFieldOffsets.push(currentFieldOffs);
			parentFieldIds.push(currentFieldId);
		}
		currentFieldOffs = getPositionUnbased();
		currentFieldId = fieldId;
		//System.out.println("writefield " + fieldId + " at " + Integer.toHexString(currentFieldOffs));
		writeShort(fieldId == null ? -1 : fieldId.ordinal());
		writeShort(0); //temp field length
	}

	public void endObject() throws IOException {
		writeShort(0xFFFF);
		writeShort(0);
	}

	public void endWriteField() throws IOException {
		align(4);
		checkpoint();
		int nowPos = getPositionUnbased();
		//System.out.println("fieldend at " + Integer.toHexString(nowPos) + " of " + currentFieldId);
		seek(currentFieldOffs + 2);
		int size = (nowPos - currentFieldOffs) >> 2;
		if (size > 0xFFFF) {
			throw new RuntimeException("Field too big!");
		}
		writeShort(size);
		if (!parentFieldOffsets.isEmpty()) {
			currentFieldOffs = parentFieldOffsets.pop();
			currentFieldId = parentFieldIds.pop();
		} else {
			currentFieldOffs = -1;
			currentFieldId = null;
		}
		resetCheckpoint();
	}

	public ILCObjTag beginReadILCObj() throws IOException {
		ILCObjTag tag = ILCObjTag.identify(readPaddedString(4));
		order(ByteOrder.BIG_ENDIAN);
		orderByBOM(readShort(), ILC_BOM_BE, ILC_BOM_LE);
		readShort();
		fileVersion = readInt();
		return tag;
	}

	@Override
	public void close() throws IOException {
		pad(16, 0x11);
		stringTable.writeTable();
		super.close();
	}
}
