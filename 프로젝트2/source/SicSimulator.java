package SP25_simulator;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.xml.transform.Source;

/**
 * 시뮬레이터로서의 작업을 담당한다. VisualSimulator에서 사용자의 요청을 받으면 이에 따라 ResourceManager에 접근하여
 * 작업을 수행한다.
 * 
 * 작성중의 유의사항 : 1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나
 * 완전히 대체하는 것은 지양할 것. 2) 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨. 3) 모든 void 타입의 리턴값은
 * 유저의 필요에 따라 다른 리턴 타입으로 변경 가능. 4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된
 * 한글은 상관 없음)
 * 
 * 
 * 
 * + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수
 * 있습니다.
 */
public class SicSimulator {
	ResourceManager rMgr;
	VisualSimulator vsim;
	InstLuncher instLuncher;
	ArrayList<Program> sections;
	Program currentSection;
	int loadLocation = -1;
	boolean isProcessing = true;

	public SicSimulator(ResourceManager resourceManager, VisualSimulator visualSimulator) {
		// 필요하다면 초기화 과정 추가
		sections = new ArrayList<>();
		this.rMgr = resourceManager;
		this.vsim = visualSimulator;
		instLuncher = new InstLuncher(resourceManager, vsim);
	}

	/**
	 * 레지스터, 메모리 초기화 등 프로그램 load와 관련된 작업 수행. 단, object code의 메모리 적재 및 해석은
	 * SicLoader에서 수행하도록 한다.
	 */
	public void load(File program) {
		/* 메모리 초기화, 레지스터 초기화 등 */
		
		//프로그램의 EXTREF를 다른 프로그램의 EXTDEF를 읽어서 절대 위치를 저장
		for(Program section : sections) {
			for(String extref : section.getExtrefs()) {
				for(Program otherSection : sections) {
					if(section.getName().equals(otherSection.getName()))
						continue;
					
					int location = otherSection.searchSymbol(extref);
					if(location >= 0) {
						section.addSymbol(extref, location);
						break;
					}
				}
					
			}
		}
		
		//프로그램의 Modification record를 읽어서 메모리값 변경
		for(Program section : sections) {
			for(Modification mod : section.getModifications()) {
				int start = section.getStart() + mod.getOffset();
				int modByteSize = mod.getLength() / 2;
				if(mod.getLength() % 2 == 1)
					modByteSize += 1;
				
				int location = section.searchSymbol(mod.getSymbol());
				long mask = (1L << (mod.getLength() * 4)) - 1;
				location = location & (int)mask;
			    byte[] addition = new byte[modByteSize];
			    
			    for(int i = 0; i < modByteSize; ++i) {
			    	addition[i] = (byte)((location >> 8 * (modByteSize - 1 - i)) & 0xFF);
			    }
			    if(mod.getIsNegative())
			    	rMgr.subMemory(start, addition, modByteSize);
			    else {
			    	rMgr.addMemory(start, addition, modByteSize);
				}
			}
		}
	}
	
	/**
	 * 1개의 instruction이 수행된 모습을 보인다.
	 */
	public void oneStep() {
		if(isProcessing == false)
			return;
		
		//PC 불러오고 명령어 길이만큼 더하기
		int pc = rMgr.getRegisterValue("PC");
		byte[] bytes = rMgr.getMemory(pc, 4);
		Command command = new Command(bytes, pc);
		rMgr.addRegisterValue("PC", command.getLength() / 2);
		
		//명령어 실행
		isProcessing = instLuncher.processOperator(command);
		
		//현재 실행중인 프로그램 탐색
		for(Program program : sections) {
			int programStart = program.getStart();
			int programEnd = program.getStart() + program.getLength();
			if(pc >= programStart && pc < programEnd) {
				currentSection = program;
				break;
			}
		}
		
		//GUI 업데이트
		String operatorString = Command.operators.get(command.getOperator()).name;
		vsim.selectCommand(command);
		vsim.addOperatorLog(operatorString);
		vsim.showHeader(currentSection);
		vsim.showAllRegisters();
		vsim.showTargetAddr();
	}

	/**
	 * 남은 모든 instruction이 수행된 모습을 보인다.
	 */
	public void allStep() {
		//프로세스가 끝날때까지 oneStep 실행
		while(isProcessing) {
			oneStep();
		}
	}

	/**
	 * 각 단계를 수행할 때 마다 관련된 기록을 남기도록 한다.
	 */
	public void addLog(String log) {
	}
	
	
	
	
	//메모리에서 받은 바이트들로 다수의 명령어 코드 추출
	public ArrayList<Command> parseCommands(byte[] bytes) {
		if(bytes == null || bytes.length <= 2)
			return null;
		
		ArrayList<Command> commandList = new ArrayList<Command>();
		int i = 0;
		while(i < bytes.length) {
			Command command = new Command(Arrays.copyOfRange(bytes, i, bytes.length), i);
			if(command.getLength() <= 0) {
				++i;
				continue;
			}
			commandList.add(command);
			i += command.getLength() / 2;
		}
		return commandList;
	}
	
	
	
	//새로운 프로그램의 end record를 읽었을때 해당 프로그램 추가
	public void addProgram(Program program) {
		sections.add(program);
		if(currentSection == null)
			currentSection = program;
	}
}





//프로그램(섹션) 클래스
class Program {
	private String name;
	private int start;
	private int length;
	private SymbolTable symTable;
	private ArrayList<Modification> modifications;
	
	Program(String name, int start, int length) {
		modifications = new ArrayList<>();
		symTable = new SymbolTable();
		this.name = name;
		this.start = start;
		this.length = length;
		addSymbol(name, start);
	}
	
	public String getName() {
		return name;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getLength() {
		return length;
	}
	
	public ArrayList<Modification> getModifications() {
		return modifications;
	}
	
	public ArrayList<String> getExtrefs() {
		return symTable.extrefList;
	}
	
	//심볼 추가
	public void addSymbol(String symbol, int absLocation) {
		symTable.putSymbol(symbol, absLocation);
	}
	
	//심볼 탐색
	public int searchSymbol(String symbol) {
		return symTable.search(symbol);
	}
	
	//수정 레코드 추가
	public void addModification(Modification mod) {
		modifications.add(mod);
	}
	
	//EXTREF 추가
	public void addExtref(String extref) {
		symTable.putExternalref(extref);
	}
}





//수정 레코드 클래스
class Modification {
	private int offset;
	private int length;
	private boolean isNegative;
	private String symbol;
	
	public Modification(int offset, int length, boolean isNegative, String symbol) {
		this.offset = offset;
		this.length = length;
		this.isNegative = isNegative;
		this.symbol = symbol;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getLength() {
		return length;
	}
	
	public boolean getIsNegative() {
		return isNegative;
	}
	
	public String getSymbol() {
		return symbol;
	}
}





//명령어 클래스
class Command {
	private int data;
	private int length;
	private byte operator;
	private int location;
	
	//명령어 opcode와 명령어 정보 매핑
	static HashMap<Byte, Integer> formats;
	static HashMap<Byte, Operator> operators;
	
	//nixbpe 플래그
	final static int N_FLAG = 5;
	final static int I_FLAG = 4;
	final static int X_FLAG = 3;
	final static int B_FLAG = 2;
	final static int P_FLAG = 1;
	final static int E_FLAG = 0;
	
	
	
	//메모리에서 읽은 바이트들로 하나의 명령어 생성
	public Command(byte[] bytes, int offset) {
		if(bytes == null || bytes.length <= 1)
			return;
		
		operator = parseOperator(bytes);
		Integer format = formats.get(operator);
		if(format == null)
			return;
		
		byte second_byte = bytes[1];
		if(isRightNthBitSet(bytes[0], 0) || isRightNthBitSet(bytes[0], 1)) {
			if(format != 3)
				return;
			if(isRightNthBitSet(second_byte, 4))
				length = 8;
			else {
				length = 6;
			}
		}
		else if(format == 2) {
			length = 4;
		}
		for(int i = 0; i < length / 2; ++i) {
			data |= (bytes[i] & 0xFF) << ((length / 2 - 1 - i) * 8);
		}
		this.location = offset;
	}
	
	
	
	
	public int getLength() {
		return length;
	}
	
	public byte getOperator() {
		return operator;
	}
	
	public int getLocation() {
		return location;
	}
	
	public int getData() {
		return data;
	}
	
	//명령어 길이만큼 실제 코드값 data 문자열 반환ㄴ
	public String toString() {
		long mask = (1L << (length * 4)) - 1;
	    int lower_bits = data & (int)mask;
	    return String.format("%0" + length + "X", lower_bits);
	}
	
	
	
	//메모리에서 읽은 바이트들로 opcode 파싱
	private static byte parseOperator(byte[] bytes) {
		if(bytes == null || bytes.length <= 0)
			return 0;
		
		byte operatorByte = bytes[0];
		for(int i = 0; i < 2; ++i) {
			operatorByte = clearRightNthBit(operatorByte, i);
		}
		return operatorByte;
	}
	
	
	
	//유틸리티, 오른쪽 n번째 비트가 1인지
	private static boolean isRightNthBitSet(byte data, int n) {
		byte mask = (byte)(1 << n);
        return (data & mask) != 0;
	}
	
	
	
	//유틸리티, 오른쪽 n번째 비트를 0으로
	private static byte clearRightNthBit(byte data, int n) {
		byte mask = (byte)(~(1 << n));
        return (byte)(data & mask);
	}
	
	
	
	//유틸리티, nixbpe 값이 있는지
	public boolean hasFlag(int flag) {
		int flagOffset = (length - 3) * 4 + flag;
		int mask = (1 << flagOffset);
		return (data & mask) != 0;
	}
	
	
	
	//부호 여부에 따른 피연산자값 반환
	public int getOperandValue(boolean signed) {
		int operandBitCnt = (length - 3) * 4;
		long mask = (1 << operandBitCnt) - 1;
		int operandValue = data & (int)mask;
		if ((operandValue & (1 << (operandBitCnt - 1))) != 0 && signed)
	        return operandValue - (1 << operandBitCnt);
	    return operandValue;
	}
	
	
	
	//정적 매핑 변수 초기화
	static {
	    formats = new HashMap<>();
	    formats.put((byte) 0x18, 3);
        formats.put((byte) 0x58, 3);
        formats.put((byte) 0x90, 2);
        formats.put((byte) 0x40, 3);
        formats.put((byte) 0xB4, 2);
        formats.put((byte) 0x28, 3);
        formats.put((byte) 0x88, 3);
        formats.put((byte) 0xA0, 2);
        formats.put((byte) 0x24, 3);
        formats.put((byte) 0x64, 3);
        formats.put((byte) 0x9C, 2);
        formats.put((byte) 0x3C, 3);
        formats.put((byte) 0x30, 3);
        formats.put((byte) 0x34, 3);
        formats.put((byte) 0x38, 3);
        formats.put((byte) 0x48, 3);
        formats.put((byte) 0x00, 3);
        formats.put((byte) 0x68, 3);
        formats.put((byte) 0x50, 3);
        formats.put((byte) 0x70, 3);
        formats.put((byte) 0x08, 3);
        formats.put((byte) 0x6C, 3);
        formats.put((byte) 0x74, 3);
        formats.put((byte) 0x04, 3);
        formats.put((byte) 0xD0, 3);
        formats.put((byte) 0x20, 3);
        formats.put((byte) 0x60, 3);
        formats.put((byte) 0x98, 2);
        formats.put((byte) 0x44, 3);
        formats.put((byte) 0xD8, 3);
        formats.put((byte) 0x4C, 3);
        formats.put((byte) 0xA4, 2);
        formats.put((byte) 0xA8, 2);
        formats.put((byte) 0xEC, 3);
        formats.put((byte) 0x0C, 3);
        formats.put((byte) 0x78, 3);
        formats.put((byte) 0x54, 3);
        formats.put((byte) 0x80, 3);
        formats.put((byte) 0xD4, 3);
        formats.put((byte) 0x14, 3);
        formats.put((byte) 0x7C, 3);
        formats.put((byte) 0xE8, 3);
        formats.put((byte) 0x84, 3);
        formats.put((byte) 0x10, 3);
        formats.put((byte) 0x1C, 3);
        formats.put((byte) 0x5C, 3);
        formats.put((byte) 0x94, 2);
        formats.put((byte) 0xB0, 2);
        formats.put((byte) 0xE0, 3);
        formats.put((byte) 0x2C, 3);
        formats.put((byte) 0xB8, 2);
        formats.put((byte) 0xDC, 3);
        
        operators = new HashMap<>();
        operators.put((byte) 0x18, new Operator("ADD",  (byte) 0x18, 3));
        operators.put((byte) 0x58, new Operator("ADDF", (byte) 0x58, 3));
        operators.put((byte) 0x90, new Operator("ADDR", (byte) 0x90, 2));
        operators.put((byte) 0x40, new Operator("AND",  (byte) 0x40, 3));
        operators.put((byte) 0xB4, new Operator("CLEAR",(byte) 0xB4, 2));
        operators.put((byte) 0x28, new Operator("COMP", (byte) 0x28, 3));
        operators.put((byte) 0x88, new Operator("COMPF",(byte) 0x88, 3));
        operators.put((byte) 0xA0, new Operator("COMPR",(byte) 0xA0, 2));
        operators.put((byte) 0x24, new Operator("DIV",  (byte) 0x24, 3));
        operators.put((byte) 0x64, new Operator("DIVF", (byte) 0x64, 3));
        operators.put((byte) 0x9C, new Operator("DIVR", (byte) 0x9C, 2));
        operators.put((byte) 0x3C, new Operator("J",    (byte) 0x3C, 3));
        operators.put((byte) 0x30, new Operator("JEQ",  (byte) 0x30, 3));
        operators.put((byte) 0x34, new Operator("JGT",  (byte) 0x34, 3));
        operators.put((byte) 0x38, new Operator("JLT",  (byte) 0x38, 3));
        operators.put((byte) 0x48, new Operator("JSUB", (byte) 0x48, 3));
        operators.put((byte) 0x00, new Operator("LDA",  (byte) 0x00, 3));
        operators.put((byte) 0x68, new Operator("LDB",  (byte) 0x68, 3));
        operators.put((byte) 0x50, new Operator("LDCH", (byte) 0x50, 3));
        operators.put((byte) 0x70, new Operator("LDF",  (byte) 0x70, 3));
        operators.put((byte) 0x08, new Operator("LDL",  (byte) 0x08, 3));
        operators.put((byte) 0x6C, new Operator("LDS",  (byte) 0x6C, 3));
        operators.put((byte) 0x74, new Operator("LDT",  (byte) 0x74, 3));
        operators.put((byte) 0x04, new Operator("LDX",  (byte) 0x04, 3));
        operators.put((byte) 0xD0, new Operator("LPS",  (byte) 0xD0, 3));
        operators.put((byte) 0x20, new Operator("MUL",  (byte) 0x20, 3));
        operators.put((byte) 0x60, new Operator("MULF", (byte) 0x60, 3));
        operators.put((byte) 0x98, new Operator("MULR", (byte) 0x98, 2));
        operators.put((byte) 0x44, new Operator("OR",   (byte) 0x44, 3));
        operators.put((byte) 0xD8, new Operator("RD",   (byte) 0xD8, 3));
        operators.put((byte) 0x4C, new Operator("RSUB", (byte) 0x4C, 3));
        operators.put((byte) 0xA4, new Operator("SHIFTL",(byte) 0xA4, 2));
        operators.put((byte) 0xA8, new Operator("SHIFTR",(byte) 0xA8, 2));
        operators.put((byte) 0xEC, new Operator("SSK",  (byte) 0xEC, 3));
        operators.put((byte) 0x0C, new Operator("STA",  (byte) 0x0C, 3));
        operators.put((byte) 0x78, new Operator("STB",  (byte) 0x78, 3));
        operators.put((byte) 0x54, new Operator("STCH", (byte) 0x54, 3));
        operators.put((byte) 0x80, new Operator("STF",  (byte) 0x80, 3));
        operators.put((byte) 0xD4, new Operator("STI",  (byte) 0xD4, 3));
        operators.put((byte) 0x14, new Operator("STL",  (byte) 0x14, 3));
        operators.put((byte) 0x7C, new Operator("STS",  (byte) 0x7C, 3));
        operators.put((byte) 0xE8, new Operator("STSW", (byte) 0xE8, 3));
        operators.put((byte) 0x84, new Operator("STT",  (byte) 0x84, 3));
        operators.put((byte) 0x10, new Operator("STX",  (byte) 0x10, 3));
        operators.put((byte) 0x1C, new Operator("SUB",  (byte) 0x1C, 3));
        operators.put((byte) 0x5C, new Operator("SUBF", (byte) 0x5C, 3));
        operators.put((byte) 0x94, new Operator("SUBR", (byte) 0x94, 2));
        operators.put((byte) 0xB0, new Operator("SVC",  (byte) 0xB0, 2));
        operators.put((byte) 0xE0, new Operator("TD",   (byte) 0xE0, 3));
        operators.put((byte) 0x2C, new Operator("TIX",  (byte) 0x2C, 3));
        operators.put((byte) 0xB8, new Operator("TIXR", (byte) 0xB8, 2));
        operators.put((byte) 0xDC, new Operator("WD",   (byte) 0xDC, 3));
	}
}




//연산자 클래스
class Operator {
	String name;
	byte opcode;
	int format;
	
	Operator(String name, byte opcode, int format) {
		this.name = name;
		this.opcode = opcode;
		this.format = format;
	}
}