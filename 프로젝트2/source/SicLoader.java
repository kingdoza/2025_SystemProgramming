package SP25_simulator;

import java.io.*;
import java.util.ArrayList;

/**
 * SicLoader는 프로그램을 해석해서 메모리에 올리는 역할을 수행한다. 이 과정에서 linker의 역할 또한 수행한다.
 * 
 * SicLoader가 수행하는 일을 예를 들면 다음과 같다. - program code를 메모리에 적재시키기 - 주어진 공간만큼 메모리에 빈
 * 공간 할당하기 - 과정에서 발생하는 symbol, 프로그램 시작주소, control section 등 실행을 위한 정보 생성 및 관리
 */
public class SicLoader {
	ResourceManager rMgr;
	SicSimulator sicSimulator;
	int programStart = 0;

	public SicLoader(ResourceManager resourceManager, SicSimulator sicSimulator) {
		// 필요하다면 초기화
		setResourceManager(resourceManager);
		resourceManager.initializeResource();
		this.sicSimulator = sicSimulator;
	}

	/**
	 * Loader와 프로그램을 적재할 메모리를 연결시킨다.
	 * 
	 * @param rMgr
	 */
	public void setResourceManager(ResourceManager resourceManager) {
		this.rMgr = resourceManager;
	}

	/**
	 * object code를 읽어서 load과정을 수행한다. load한 데이터는 resourceManager가 관리하는 메모리에 올라가도록
	 * 한다. load과정에서 만들어진 symbol table 등 자료구조 역시 resourceManager에 전달한다.
	 * 
	 * @param objectCode 읽어들인 파일
	 */
	public void load(File objectCode) {
		ArrayList<String> lines = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(objectCode))) {
		    String line;
		    while ((line = reader.readLine()) != null) {
		    	lines.add(line);
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
		
		Program program = null;
		int programLength = 0;
		for (String line : lines) {
			if(line.length() <= 0)
				continue;
			if(line.charAt(0) == 'H') {			//헤더 레코드, 프로그램 생성
				programLength = Integer.parseInt(line.substring(13, 19), 16);
				int programOffset = Integer.parseInt(line.substring(7, 13), 16);
				String programName = line.substring(1, 7).trim();
				program = new Program(programName, programStart + programOffset, programLength);
			}
			else if(line.charAt(0) == 'T') {	//텍스트 레코드, 가상 메모리 공간에 로드
				loadTextCode(line);
			}
			else if(line.charAt(0) == 'M') {	//수정 레코드, 수정 레코드 클래스로 저장하고 로드 후 메모리 변경
				storeMods(line, program);
			}
			else if(line.charAt(0) == 'R') {	//EXTREF 레코드, 프로그램 심볼 테이블에 임시 저장
				storeExtrefs(line, program);
			}
			else if(line.charAt(0) == 'D') {	//EXTDEF 레코드, 프로그램 심볼 테이블에 위치와 함께 저장
				storeExtdefs(line, program);
			}
			else if(line.charAt(0) == 'E') {	//End 레코드, 여태 저장한 프로그램 정보들을 시뮬레이터에 추가
				programStart += programLength;
				sicSimulator.addProgram(program);
				if(line.length() > 1) {
					sicSimulator.loadLocation = Integer.parseInt(line.substring(1), 16);
				}
			}
		}
	}
	
	
	
	//수정 레코드 줄을 읽어서 수정 레코드 클래스로 파싱
	private void storeMods(String line, Program program) {
		int offset = Integer.parseInt(line.substring(1, 7), 16);
		int length = Integer.parseInt(line.substring(7, 9), 16);
		boolean isNegative = (line.charAt(9) == '-');
		String symbol = line.substring(10);
		program.addModification(new Modification(offset, length, isNegative, symbol));
	}
	
	
	
	//EXTREF 레코드 줄을 읽어서 프로그램 심볼 테이블에 임시 저장, 추후에 절대위치와 함께 삽입
	private void storeExtrefs(String line, Program program) {
		int i = 1;
		while(i < line.length()) {
			String name = line.substring(i, i + 6);
			i += 6;
			program.addExtref(name.trim());
		}
	}
	
	
	
	//EXTDEF 레코드 줄을 읽어서 프로그램 심볼 테이블에 위치와 함께 저장
	private void storeExtdefs(String line, Program program) {
		int i = 1;
		while(i < line.length()) {
			String name = line.substring(i, i + 6);
			i += 6;
			int location = Integer.parseInt(line.substring(i, i + 6), 16);
			i += 6;
			location += program.getStart();
			program.addSymbol(name.trim(), location);
		}
	}
	
	
	
	//텍스트 레코드 줄을 읽어서 명령어 코드 부분을 리소스 매니저에 로드
	private void loadTextCode(String line) {
		int start = programStart + Integer.parseInt(line.substring(1, 7), 16);
		int length = Integer.parseInt(line.substring(7, 9), 16);
		String texts = line.substring(9);
		
		byte[] bytes = new byte[length];
		for(int i = 0; i < length; ++i) {
			bytes[i] = (byte)Integer.parseInt(texts.substring(2 * i, 2 * i + 2), 16);
			
		}
		
		rMgr.setMemory(start, bytes, length);
	}
}
