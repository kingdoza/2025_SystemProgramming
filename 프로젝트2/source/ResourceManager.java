	package SP25_simulator;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * ResourceManager는 컴퓨터의 가상 리소스들을 선언하고 관리하는 클래스이다. 크게 네가지의 가상 자원 공간을 선언하고, 이를
 * 관리할 수 있는 함수들을 제공한다.
 * 
 * 
 * 1) 입출력을 위한 외부 장치 또는 device 2) 프로그램 로드 및 실행을 위한 메모리 공간. 여기서는 64KB를 최대값으로 잡는다.
 * 3) 연산을 수행하는데 사용하는 레지스터 공간. 4) SYMTAB 등 simulator의 실행 과정에서 사용되는 데이터들을 위한 변수들.
 * 
 * 2번은 simulator위에서 실행되는 프로그램을 위한 메모리공간인 반면, 4번은 simulator의 실행을 위한 메모리 공간이라는 점에서
 * 차이가 있다.
 */
public class ResourceManager {
	/**
	 * 디바이스는 원래 입출력 장치들을 의미 하지만 여기서는 파일로 디바이스를 대체한다. 즉, 'F1'이라는 디바이스는 'F1'이라는 이름의
	 * 파일을 의미한다. deviceManager는 디바이스의 이름을 입력받았을 때 해당 이름의 파일 입출력 관리 클래스를 리턴하는 역할을 한다.
	 * 예를 들어, 'A1'이라는 디바이스에서 파일을 read모드로 열었을 경우, hashMap에 <"A1", scanner(A1)> 등을
	 * 넣음으로서 이를 관리할 수 있다.
	 * 
	 * 변형된 형태로 사용하는 것 역시 허용한다. 예를 들면 key값으로 String대신 Integer를 사용할 수 있다. 파일 입출력을 위해
	 * 사용하는 stream 역시 자유로이 선택, 구현한다.
	 * 
	 * 이것도 복잡하면 알아서 구현해서 사용해도 괜찮습니다.
	 */
	HashMap<String, RandomAccessFile> deviceManager = new HashMap<>();
	byte[] memory = new byte[65536]; // String으로 수정해서 사용하여도 무방함. char 2byte 이므로 char => byte
	
	//레지스터 이름, 레지스터 값 매핑
	HashMap<String, Integer> registers = new HashMap<String, Integer>();
	//레지스터 이름, 레지스터 번호 매핑
	HashMap<Integer, String> registerNum = new HashMap<Integer, String>();
	int[] register = new int[10];
	double register_F;
	//마지막 텍스트가 저장되고난 뒤의 파일 오프셋 위치
	int offset = 0;

	SymbolTable symtabList;
	// 이외에도 필요한 변수 선언해서 사용할 것.

	/**
	 * 메모리, 레지스터등 가상 리소스들을 초기화한다.
	 */
	public void initializeResource() {
		//레지스터 매핑 변수들 초기화
		Arrays.fill(memory, (byte) 0);
		registers.put("A", 0);
		registers.put("X", 0);
		registers.put("L", 0);
		registers.put("B", 0);
		registers.put("S", 0);
		registers.put("T", 0);
		registers.put("F", 0);
		registers.put("PC", 0);
		registers.put("SW", 0);
		registers.put("TA", 0);
		
		registerNum.put(0, "A");
		registerNum.put(1, "X");
		registerNum.put(2, "L");
		registerNum.put(3, "B");
		registerNum.put(4, "S");
		registerNum.put(5, "T");
		registerNum.put(6, "F");
		registerNum.put(8, "PC");
		registerNum.put(9, "SW");
	}
	
	
	
	//레지스터 이름으로 레지스터 값 반환
	public int getRegisterValue(String register) {
		if(register.equals("CH")) {
			return (registers.get("A") & 0xFF);
		}
		return registers.get(register);
	}
	
	
	
	//레지스터 이름으로 레지스터 값 세팅
	public void setRegisterValue(String register, int num) {
		if(register.equals("CH")) {
			int newARegValue = (registers.get("A") & 0xFFFFFF00) | (num & 0xFF);
			registers.put("A", newARegValue);
		}
		registers.put(register, num);
	}
	
	
	
	//레지스터 이름으로 레지스터 값 더하기
	public int addRegisterValue(String register, int addition) {
		if(register.equals("CH")) {
			return 0;
		}
		return registers.put(register, registers.get(register) + addition);
	}

	/**
	 * deviceManager가 관리하고 있는 파일 입출력 stream들을 전부 종료시키는 역할. 프로그램을 종료하거나 연결을 끊을 때
	 * 호출한다.
	 */
	public void closeDevice() {
		for(RandomAccessFile file : deviceManager.values()) {
			try {
				file.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * 디바이스를 사용할 수 있는 상황인지 체크. TD명령어를 사용했을 때 호출되는 함수. 입출력 stream을 열고 deviceManager를
	 * 통해 관리시킨다.
	 * 
	 * @param devName 확인하고자 하는 디바이스의 번호,또는 이름
	 */
	public void testDevice(String devName) {
		if(deviceManager.get(devName) != null)
			return;
		try {
			deviceManager.put(devName, new RandomAccessFile(devName, "rw"));
		} catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }
	}

	/**
	 * 디바이스로부터 원하는 개수만큼의 글자를 읽어들인다. RD명령어를 사용했을 때 호출되는 함수.
	 * 
	 * @param devName 디바이스의 이름
	 * @param num     가져오는 글자의 개수
	 * @return 가져온 데이터
	 */
	public byte readDevice(String devName) {
		try {
			return deviceManager.get(devName).readByte();
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * 디바이스로 원하는 개수 만큼의 글자를 출력한다. WD명령어를 사용했을 때 호출되는 함수.
	 * 
	 * @param devName 디바이스의 이름
	 * @param data    보내는 데이터
	 * @param num     보내는 글자의 개수
	 */
	public void writeDevice(String devName, byte data) {
		try {
			RandomAccessFile file = deviceManager.get(devName);
			if(file.getFilePointer() == 0) {
				file.setLength(0);
			}
			file.writeByte(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 메모리의 특정 위치에서 원하는 개수만큼의 글자를 가져온다.
	 * 
	 * @param location 메모리 접근 위치 인덱스
	 * @param num      데이터 개수
	 * @return 가져오는 데이터
	 */
	public byte[] getMemory(int location, int num) {
		byte[] data = new byte[num];
		for(int i = location; i < location + num; ++i) {
			if(i < 0 || i >= memory.length) {
				return data;
			}
			data[i - location] = memory[i];
		}
		return data;

	}
	
	
	
	//메모리 영역의 바이트 배열을 정수로 변화 후 리턴
	public int getMemoryToInt(int location, int num) {
		int memoryValue = 0;
		
		byte[] bytesLoad = getMemory(location, Math.min(num, 4));
		for(int i = 0; i < bytesLoad.length; ++i) {
			int shifts = (bytesLoad.length - 1 - i) * 8;
			memoryValue |= (bytesLoad[i] & 0xFF) << shifts;
		}
		return memoryValue;
	}

	/**
	 * 메모리의 특정 위치에 원하는 개수만큼의 데이터를 저장한다.
	 * 
	 * @param locate 접근 위치 인덱스
	 * @param data   저장하려는 데이터
	 * @param num    저장하는 데이터의 개수
	 */
	public void setMemory(int locate, byte[] data, int num) {
		for(int i = locate; i < locate + num; ++i) {
			if(i < 0 || i >= memory.length) {
				return;
			}
			memory[i] = data[i - locate];
		}
		offset = locate + num;
	}
	
	
	
	//정수에서 바이트 배열로 변화 후 메모리 영역에 저장
	public void setMemoryFromInt(int locate, int memoryValue, int num) {
		byte[] bytesStore = new byte[Math.min(num, 4)];
		
		for(int i = 0; i < bytesStore.length; ++i) {
			int shifts = (bytesStore.length - 1 - i) * 8;
			bytesStore[i] = (byte)((memoryValue >> shifts) & 0xFF);
		}
		setMemory(locate, bytesStore, bytesStore.length);
	}
	
	
	
	//해당 메모리 영역에 addtion만큼 더하기, 수정 레코드 부분에서 주로 사용
	public void addMemory(int locate, byte[] addition, int num) {
		for(int i = locate; i < locate + num; ++i) {
			if(i < 0 || i >= memory.length) {
				return;
			}
			memory[i] += addition[i - locate];
		}
	}
	
	
	
	//해당 메모리 영역에 sub만큼 빼기, 수정 레코드 부분에서 주로 사용
	public void subMemory(int locate, byte[] sub, int num) {
		for(int i = locate; i < locate + num; ++i) {
			if(i < 0 || i >= memory.length) {
				return;
			}
			memory[i] -= sub[i - locate];
		}
	}

	/**
	 * 번호에 해당하는 레지스터가 현재 들고 있는 값을 리턴한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * 
	 * @param regNum 레지스터 분류번호
	 * @return 레지스터가 소지한 값
	 */
	public int getRegister(int regNum) {
		String regName = registerNum.get(regNum);
		return registers.get(regName);

	}

	/**
	 * 번호에 해당하는 레지스터에 새로운 값을 입력한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * 
	 * @param regNum 레지스터의 분류번호
	 * @param value  레지스터에 집어넣는 값
	 */
	public void setRegister(int regNum, int value) {
		String regName = registerNum.get(regNum);
		registers.put(regName, value);
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. int값을 char[]형태로 변경한다.
	 * 
	 * @param data
	 * @return
	 */
	public char[] intToChar(int data) {
		return null;
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. char[]값을 int형태로 변경한다.
	 * 
	 * @param data
	 * @return
	 */
	public int byteToInt(byte[] data) {
		return 0;
	}
}