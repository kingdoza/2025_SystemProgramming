import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.Soundbank;

import java.io.*;


/**
 * Assembler : 
 * 이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 프로그램의 수행 작업은 다음과 같다. <br>
 * 1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다. <br>
 * 2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다. <br>
 * 3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1) <br>
 * 4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2) <br>
 * 
 * <br><br>
 * 작성중의 유의사항 : <br>
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 안된다.<br>
 *  2) 마찬가지로 작성된 코드를 삭제하지 않으면 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.<br>
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.<br>
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)<br>
 * 
 * <br><br>
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
	/** instruction 명세를 저장한 공간 */
	InstTable instTable;
	/** 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간. */
	ArrayList<String> lineList;
	/** 프로그램의 section별로 symbol table을 저장하는 공간*/
	ArrayList<SymbolTable> symtabList;
	/** 프로그램의 section별로 프로그램을 저장하는 공간*/
	ArrayList<TokenTable> TokenList;
	/** 프로그램의 section별로 literal table을 저장하는 공간*/
	ArrayList<LiteralTable> littabList;
	/** 
	 * Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간. <br>
	 * 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
	 */
	//section별 ObjectCode 리스트
	ArrayList<ObjectProgram> codeList;
	//symtabList, littabList, TokenList에 접근하기 위한 section index 해시맵
	HashMap<String, Integer> sectionList;
	//현재 section 이름
	String section;
	//section 개수
	int section_counter;
	//location counter
	int locctr;
	
	/**
	 * 클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
	 * 
	 * @param instFile : instruction 명세를 작성한 파일 이름. 
	 */
	public Assembler(String instFile) {
		instTable = new InstTable(instFile);
		lineList = new ArrayList<String>();
		symtabList = new ArrayList<SymbolTable>();
		littabList = new ArrayList<LiteralTable>();
		TokenList = new ArrayList<TokenTable>();
		codeList = new ArrayList<ObjectProgram>();
		sectionList = new HashMap<String, Integer>();
		Token.instTable = instTable;
	}
	
	
	//코드 변환 시 에러가 발생하면 에러 내용을 출력하고 즉시 프로그램 종료
	public static void exitError(String errorString) {
		System.out.println("----- " + errorString + " -----");
		System.exit(1);
	}

	/** 
	 * 어셐블러의 메인 루틴
	 */
	public static void main(String[] args) {
		Assembler assembler = new Assembler("inst_table.txt");
		assembler.loadInputFile("input.txt");
		
		if(assembler.pass1() == false) {
			exitError("pass1 error");
		}
		assembler.printSymbolTable("output_symtab.txt");
		assembler.printLiteralTable("output_littab.txt");
		//assembler.printSymbolTable(null);
		//assembler.printLiteralTable(null);
		
		assembler.pass2();
		assembler.printObjectCode("output_objectcode.txt");
		//assembler.printObjectCode(null);
	}


	/**
	 * inputFile을 읽어들여서 lineList에 저장한다.<br>
	 * @param inputFile : input 파일 이름.
	 */
	private void loadInputFile(String inputFile) {
		// TODO Auto-generated method stub
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	/**
	 * 작성된 SymbolTable들을 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printSymbolTable(String fileName) {
		PrintStream ps = getPrintStream(fileName);
		for(int i = 0; i < section_counter; ++i) {
			if(symtabList.get(i).symbolList.size() <= 0)
				continue;
			symtabList.get(i).print(ps);
			ps.println();
		}
	}

	/**
	 * 작성된 LiteralTable들을 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printLiteralTable(String fileName) {
		PrintStream ps = getPrintStream(fileName);
		for(int i = 0; i < section_counter; ++i) {
			if(littabList.get(i).literalList.size() <= 0)
				continue;
			littabList.get(i).print(ps);
			ps.println();
		}
	}

	/** 
	 * pass1 과정을 수행한다.<br>
	 *   1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성<br>
	 *   2) label을 symbolTable에 정리<br>
	 *   <br><br>
	 *    주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
	 */
	private boolean pass1() {
		section_counter = 0;
		// TODO Auto-generated method stub
		for(String line : lineList) {
			Token token = new Token(line, locctr);
			if(token.getParseSucceed() == false) {
				return false;
			}
			locctr = nextLocctr(token);
			String token_section = nextSection(token);
			if(token_section != section) {	//이전과 다른 section으로 진입
				if(section != null) {
					locctr = getLiteralTable().ltorg(locctr);
				}
				section = token_section;
				createNewSection(token_section);
			}
			getTokenTable().putToken(token);
			addSymbol(token);
			addLiteral(token);
			check_ltorg(token);
			check_exts(token);
			getObjectProgram().setLength(locctr);
			
			//token.print(line);
		}
		locctr = getLiteralTable().ltorg(locctr);
		getObjectProgram().setLength(locctr);
		return true;
	}
	
	/**
	 * pass2 과정을 수행한다.<br>
	 *   1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
	 */
	private void pass2() {
		// TODO Auto-generated method stub
		for(int i = 0; i < section_counter; ++i) {
			int token_list_size = TokenList.get(i).tokenList.size();
			for(int j = 0; j < token_list_size; ++j) {
				Text object_text = TokenList.get(i).makeObjectCode(j);
				codeList.get(i).addObjectText(object_text);
				//System.out.println(object_text);
			}
			
			int littab_size = littabList.get(i).literalList.size();
			//System.out.println("littab_size : " + littab_size);
			for(int j = 0; j < littab_size; ++j) {
				Text object_text = littabList.get(i).getCodeText(j);
				codeList.get(i).addObjectText(object_text);
				//System.out.println(object_text);
			}
			
			for(Token token : TokenList.get(i).tokenList) {
				codeList.get(i).appendMods(token.modRecords);
			}
			codeList.get(i).setExtdefLocation(symtabList.get(i));
			codeList.get(i).sortTexts();
			//codeList.get(i).printTexts();
		}
	}
	
	
	
	//fileName으로 null을 주면 표준출력, 이외에는 해당 파일의 출력스트림으로 변환
	private PrintStream getPrintStream(String fileName) {
		PrintStream ps = System.out;
		try {
			if(fileName != null)
				ps = new PrintStream(fileName);
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
		return ps;
	}

	/**
	 * 작성된 codeList를 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printObjectCode(String fileName) {
		// TODO Auto-generated method stub
		PrintStream ps = getPrintStream(fileName);
		for(int i = 0; i < section_counter; ++i) {
			codeList.get(i).print(ps);
			ps.println();
		}
	}
	
	
	
	//현재 section의 토큰 테이블 반환
	private TokenTable getTokenTable() {
		int sec_idx = sectionList.get(section);
		return TokenList.get(sec_idx);
	}
	
	
	
	//현재 section의 리터럴 테이블 반환
	private LiteralTable getLiteralTable() {
		int sec_idx = sectionList.get(section);
		return littabList.get(sec_idx);
	}
	
	
	
	//현재 section의 심볼 테이블 반환
	private SymbolTable getSymbolTable() {
		int sec_idx = sectionList.get(section);
		return symtabList.get(sec_idx);
	}
	
	
	
	//현재 section의 ObjectProgram 반환
	private ObjectProgram getObjectProgram() {
		int sec_idx = sectionList.get(section);
		return codeList.get(sec_idx);
	}
	
	
	
	//토큰의 명령어가 ltorg인지 확인하고 맞으면 리터럴을 object code로 변환하고 배치
	private void check_ltorg(Token token) {
		if(token.operator != null && token.operator.equals("LTORG")) {
			locctr = getLiteralTable().ltorg(locctr);
		}
	}
	
	
	
	//토큰의 명령어가 extref, extdef인지 검사하고 맞으면 extref, extdef를 필요로하는 곳에 레이블 전달
	private void check_exts(Token token) {
		if(token == null || token.operator == null)
			return;
		
		if(token.operator.equals("EXTREF")) {
			for(String operand : token.operands) {
				getObjectProgram().addExtref(operand);
				getSymbolTable().addExtref(operand);
			}
		}
		else if(token.operator.equals("EXTDEF")) {
			for(String operand : token.operands) {
				getObjectProgram().addExtdef(operand, 0);
			}
		}
	}
	
	
	
	//토큰을 분석해서 레이블이 있을 경우에는 심볼 테이블에 추가
	private void addSymbol(Token token) {
		if(token.label == null || token.label.equals(""))
			return;
		
		if(token.operator.equals("EQU")) {
			getSymbolTable().putSymbol(token.label, token.getOperandsResult(getSymbolTable(), getLiteralTable()));
			return;
		}
		getSymbolTable().putSymbol(token.label, token.location);
	}
	
	
	
	//토큰을 분석해서 리터럴이 있을 경우에는 리터럴 테이블에 추가
	private void addLiteral(Token token) {
		if(token.operands.length <= 0)
			return;
		if(token.operands[0].charAt(0) != '=')
			return;
		getLiteralTable().putLiteral(token.operands[0], token.location);
	}
	
	
	
	//새로운 section에 진입할 때 리터럴 테이블, 심볼 테이블, ObjectProgram 등을 할당
	private void createNewSection(String new_section) {
		sectionList.put(new_section, section_counter++);
		symtabList.add(new SymbolTable(new_section));
		littabList.add(new LiteralTable());
		codeList.add(new ObjectProgram(new_section, locctr, section_counter == 1));
		
		int sec_idx = sectionList.get(new_section);
		TokenList.add(new TokenTable(symtabList.get(sec_idx), littabList.get(sec_idx), instTable));
	}
	
	
	
	//토큰의 연산자가 START, CSECT인지를 확인해서 다음 section을 설정
	private String nextSection(Token token) {
		String operator = token.operator;
		String[] operands = token.operands;
		if(operator == null || operands == null)
			return section;
	
		if(operator.equals("START") || operator.equals("CSECT")) {
			if(token.label != null)
				return token.label;
		}
		return section;
	}
	
	
	
	//토큰의 형식과 연산자를 분석해서 다음 locctr를 설정
	private int nextLocctr(Token token) throws NumberFormatException {
		String operator = token.operator;
		String[] operands = token.operands;
		if(operator == null || operands == null)
			return locctr;
		
		if(operator.equals("RESB") || operator.equals("RESW")) {
			int locgap = Integer.parseInt(operands[0]) * token.instruction.format;
			token.setLocIncrement(locgap);
			return locctr + locgap;
		}
		else if(operator.equals("BYTE")) {
			int locgap = LiteralTable.getLiteralLength(operands[0]) / 2;
			token.setLocIncrement(locgap);
			return locctr + locgap;
		}
		else if(operator.equals("START")) {
			int start_loc = Integer.parseInt(operands[0]);
			token.setLocation(start_loc);
			return start_loc;
		}
		else if(operator.equals("CSECT")) {
			token.setLocation(0);
			return 0;
		}
		
		token.setLocIncrement(token.instruction.format);
		return locctr + token.instruction.format;
	}
}
