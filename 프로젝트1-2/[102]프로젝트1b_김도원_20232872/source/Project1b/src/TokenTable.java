import java.util.*;

import javax.sound.midi.Soundbank;

/**
 * 사용자가 작성한 프로그램 코드를 단어별로 분할 한 후, 의미를 분석하고, 최종 코드로 변환하는 과정을 총괄하는 클래스이다. <br>
 * pass2에서 object code로 변환하는 과정은 혼자 해결할 수 없고 symbolTable과 instTable의 정보가 필요하므로 이를 링크시킨다.<br>
 * section 마다 인스턴스가 하나씩 할당된다.
 *
 */
public class TokenTable {
	public static final int MAX_OPERAND=3;
	
	/* bit 조작의 가독성을 위한 선언 */
	public static final int nFlag=32;
	public static final int iFlag=16;
	public static final int xFlag=8;
	public static final int bFlag=4;
	public static final int pFlag=2;
	public static final int eFlag=1;
	
	/* Token을 다룰 때 필요한 테이블들을 링크시킨다. */
	SymbolTable symTab;
	LiteralTable littab;
	InstTable instTab;
	
	
	/** 각 line을 의미별로 분할하고 분석하는 공간. */
	ArrayList<Token> tokenList;
	
	/**
	 * 초기화하면서 symTable과 instTable을 링크시킨다.
	 * @param symTab : 해당 section과 연결되어있는 symbol table
	 * @param instTab : instruction 명세가 정의된 instTable
	 */
	public TokenTable(SymbolTable symTab, LiteralTable littab, InstTable instTab) {
		//...
		tokenList = new ArrayList<Token>();
		this.symTab = symTab;
		this.littab = littab;
		this.instTab = instTab;
	}
	
	/**
	 * 일반 문자열을 받아서 Token단위로 분리시켜 tokenList에 추가한다.
	 * @param line : 분리되지 않은 일반 문자열
	 */
	public void putToken(Token token) {
		tokenList.add(token);
	}
	
	/**
	 * tokenList에서 index에 해당하는 Token을 리턴한다.
	 * @param index
	 * @return : index번호에 해당하는 코드를 분석한 Token 클래스
	 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}
	
	/**
	 * Pass2 과정에서 사용한다.
	 * instruction table, symbol table 등을 참조하여 objectcode를 생성하고, 이를 저장한다.
	 * @param index
	 */
	public Text makeObjectCode(int index){
		//...
		return tokenList.get(index).makeObjectCode(symTab, littab);
	}
	
	/** 
	 * index번호에 해당하는 object code를 리턴한다.
	 * @param index
	 * @return : object code
	 */
	public String getObjectCode(int index) {
		return tokenList.get(index).objectCode;
	}
	
}

/**
 * 각 라인별로 저장된 코드를 단어 단위로 분할한 후  의미를 해석하는 데에 사용되는 변수와 연산을 정의한다. 
 * 의미 해석이 끝나면 pass2에서 object code로 변형되었을 때의 바이트 코드 역시 저장한다.
 */
class Token{
	//의미 분석 단계에서 사용되는 변수들
	int location;
	String label;
	String operator;
	String[] operands;
	String comment;
	char nixbpe;
	//locctr 증가치
	int locIncrement;
	//파싱 성공 여부
	boolean isParseSucceed;
	//토큰의 operator에 해당하는 명령어 정보
	Instruction instruction;
	//modification record 리스트
	ArrayList<Modification> modRecords;
	
	//명령어 검색을 위해 참조할 명령어 테이블
	static InstTable instTable;
	//레지스터 번호 해시맵
	static HashMap<String, Integer> registerMap;
	static {
	    registerMap = new HashMap<String, Integer>();
	    registerMap.put("A", 0);
	    registerMap.put("X", 1);
	    registerMap.put("L", 2);
	    registerMap.put("B", 3);
	    registerMap.put("S", 4);
	    registerMap.put("T", 5);
	    registerMap.put("F", 6);
	    registerMap.put("PC", 8);
	    registerMap.put("SW", 9);
	}

	// object code 생성 단계에서 사용되는 변수들 
	String objectCode;
	int byteSize;
	
	/**
	 * 클래스를 초기화 하면서 바로 line의 의미 분석을 수행한다. 
	 * @param line 문장단위로 저장된 프로그램 코드
	 */
	public Token(String line, int locctr) {
		//initialize 추가
		location = locctr;
		operands = new String[] {};
		modRecords = new ArrayList<Modification>();
		setFlag(TokenTable.nFlag, 1);
		setFlag(TokenTable.iFlag, 1);
		setFlag(TokenTable.pFlag, 1);
		parsing(line);
	}
	
	/**
	 * line의 실질적인 분석을 수행하는 함수. Token의 각 변수에 분석한 결과를 저장한다.
	 * @param line 문장단위로 저장된 프로그램 코드.
	 */
	public void parsing(String line) {
		List<String> splits = Arrays.asList(line.split("\t+"));
		isParseSucceed = false;
		if(splits.size() <= 0)
			return;
		isParseSucceed = true;
		if(splits.get(0).equals("."))
			return;
		Iterator<String> iter = splits.iterator();
		if(instTable.search(splits.get(0)) == null) {	//첫 번째 단어가 명령어가 아니면 레이블 파싱 시도
			isParseSucceed = isParseSucceed && iter.hasNext() && parseLabel(iter.next());
		}
		isParseSucceed = isParseSucceed && iter.hasNext() && parseOperator(iter.next());	//명령어 파싱
		if(iter.hasNext()) {	//다음 iterator가 존재하면 피연산자 파싱
			isParseSucceed = isParseSucceed && parseOperands(iter.next());	
		}
		setByteSize();
	}
	
	/** 
	 * n,i,x,b,p,e flag를 설정한다. <br><br>
	 * 
	 * 사용 예 : setFlag(nFlag, 1); <br>
	 *   또는     setFlag(TokenTable.nFlag, 1);
	 * 
	 * @param flag : 원하는 비트 위치
	 * @param value : 집어넣고자 하는 값. 1또는 0으로 선언한다.
	 */
	public void setFlag(int flag, int value) {
		//...
		if(value != 0 && value != 1) return;
		int exponent = (int) (Math.log(flag) / Math.log(2));
		if(value == 1) {
			nixbpe |= 1 << exponent;
		}
		else {
			nixbpe = (char)(nixbpe & ~(1 << exponent));
		}
	}
	
	/**
	 * 원하는 flag들의 값을 얻어올 수 있다. flag의 조합을 통해 동시에 여러개의 플래그를 얻는 것 역시 가능하다 <br><br>
	 * 
	 * 사용 예 : getFlag(nFlag) <br>
	 *   또는     getFlag(nFlag|iFlag)
	 * 
	 * @param flags : 값을 확인하고자 하는 비트 위치
	 * @return : 비트위치에 들어가 있는 값. 플래그별로 각각 32, 16, 8, 4, 2, 1의 값을 리턴할 것임.
	 */
	public int getFlag(int flags) {
		return nixbpe & flags;
	}
	
	
	
	//locIncrement setter 함수
	public void setLocIncrement(int locIncrement) {
		this.locIncrement = locIncrement;
	}
	
	
	
	//isParseSucceed getter 함수
	public boolean getParseSucceed() {
		return isParseSucceed;
	}
	
	
	
	//레이블 파싱
	private boolean parseLabel(String label) {
		this.label = label;
		return true;
	}
	
	
	
	//명령어 파싱
	private boolean parseOperator(String operator) {
		this.operator = operator;
		String searchTarget = operator;
		if(operator.length() > 0 && operator.charAt(0) == '+') {
			instruction = instTable.search(operator.substring(1));
			setFlag(TokenTable.eFlag, 1);
			setFlag(TokenTable.pFlag, 0);
			setFlag(TokenTable.bFlag, 0);
			instruction.operator = operator;
			instruction.format = 4;
		}
		else {
			instruction = instTable.search(operator);
		}
		
		if(operator.equals("BYTE") || operator.equals("WORD") || instruction.format <= 2) {
			nixbpe = 0;
		}
		return (instruction != null);
	}
	
	

	//쉼표(,)를 기준으로 피연산자 파싱, @ # X 유뮤에 따라 n i x 비트 설정
	private boolean parseOperands(String operands) {
		if(instruction.operandCount == 0) {
			setFlag(TokenTable.pFlag, 0);
			setFlag(TokenTable.bFlag, 0);
			return true;
		}
		this.operands = operands.split(",");
		for (int i = 0; i < this.operands.length; i++) {
			this.operands[i] = this.operands[i].trim();
        }
		
		if(this.operands.length <= 0)
			return false;
		
		if(this.operands[0].charAt(0) == '#') {	//#붙어 있으면 immediate mode
			setFlag(TokenTable.nFlag, 0);
			this.operands[0] = this.operands[0].substring(1);
		}
		else if(this.operands[0].charAt(0) == '@') {	//@붙어 있으면 indirect mode
			setFlag(TokenTable.iFlag, 0);
			this.operands[0] = this.operands[0].substring(1);
		}
		
		if(instruction.operandCount == -1 || instruction.operandCount == this.operands.length)
			return true;
		String lastOperand = this.operands[this.operands.length - 1];
		if((instruction.operandCount == this.operands.length - 1) && lastOperand.equals("X")) {	
			setFlag(TokenTable.xFlag, 1);	//피연산자의 개수만큼 파싱하고나서 X가 남는다면 x flag on
			this.operands = Arrays.copyOf(this.operands, this.operands.length - 1);
			return true;
		}
		return false;
	}
	
	
	
	//토큰을 object code 로 변환 시에 차지할 바이트 수
	public void setByteSize() {
		if(operator == null || instruction.format <= 0 || operator.equals("RESW") || operator.equals("RESB")) {
			byteSize = 0;
		}
		else if(operator.equals("BYTE")) {
			byteSize = LiteralTable.getLiteralLength(operands[0]) / 2;
		}
		else {
			byteSize = instruction.format;
		}
	}
	
	
	
	//location setter 함수
	public void setLocation(int location) {
		this.location = location;
	}
	
	
	
	//2형식 레지스트 연산 토큰의 피연산자 레지스터 값 반환
	private int getFormat2Result() {
		int result = 0;
		for(int i = 0; i < 2 && i < operands.length; ++i) {
			int registerNum = registerMap.get(operands[i]);
			result |= registerNum << ((1 - i) * 4);
		}
		return result;
	}
	
	
	
	//피연산자를 정수 변환 시도, 성공시 정수값 반환
	private int tryParseInt(String part) {
		try {
			int num = Integer.parseInt(part);
			setFlag(TokenTable.pFlag, 0);
			return num;
		} catch (NumberFormatException e) {
			return -1;
		}
	}
	
	
	
	//피연산자를 바이트 문자열 변환 시도(ex. C'EOF', X'05'), 성공시 문자열 바이트 반환
	private int tryParseBytes(String part) {
		if(operator.equals("BYTE")) {
			String byte_string = LiteralTable.getLiteralString(part);
			return LiteralTable.getLiteralCode(byte_string);
		}
		return -1;
	}
	
	
	
	//리터럴 테이블을 검색해서 피연산자를 리터럴로 변환 시도, 성공시 리터럴 위치 반환
	private int trySearchLiteral(String part, LiteralTable littab) {
		if(part.charAt(0) == '=') {
			String part_literal = LiteralTable.getLiteralString(part);
			return littab.searchLiteral(part_literal);
		}
		return -1;
	}
	
	
	
	//심볼 테이블을 검색해서 피연산자를 심볼로 변환 시도, 성공시 심볼 위치 반환
	private int trySearchSymbol(String part, SymbolTable symtab) {
		return symtab.searchSymbol(part);
	}
	
	
	
	//피연산자를 정수변환, 문자열 변환, 심봄 변환, 리터럴 변환, EXTREF 레이블 검색을 시도하면서 값을 계산
	private int getOperandPartValue(String part, SymbolTable symtab, LiteralTable littab, boolean is_negative) {
		int value;
		if(part.equals("*"))
			return location;
	    value = tryParseInt(part);
	    if (value >= 0) 
	    	return value;
	    value = tryParseBytes(part);
	    if (value >= 0) 
	    	return value;
	    value = trySearchLiteral(part, littab);
	    if (value >= 0) 
	    	return value;
	    value = trySearchSymbol(part, symtab);
	    if (value >= 0)
	    	return value;
	    
	    value = symtab.searchExtref(part);
	    if(value >= 0) {
	    	createModification(part, is_negative);
	    	return value;
	    }
	    Assembler.exitError("invalid operand : " + part);
	    return -1;
	}
	
	
	
	//부호와 참조 이름을 받아서 토큰 내부의 Modification record 리스트에 추가
	private void createModification(String part, boolean is_negative) {
		int mod_length = 2 * byteSize - 3;
		if(operator.equals("BYTE") || operator.equals("WORD")) {
			mod_length = 2 * byteSize;
		}
		int mod_offset = (2 * byteSize - mod_length) / 2;
		char operator = is_negative ? '-' : '+';
		Modification mod_record = new Modification(location + mod_offset, mod_length, operator, part);
		modRecords.add(mod_record);
	}
	
	
	
	//+-가 포함된 수식 피연산자의 전체값을 계산
	public int getOperandsResult(SymbolTable symtab, LiteralTable littab) {
		if(instruction.format == 2)
			return getFormat2Result();
		if(operands.length != 1)
			return 0;
		
		int result = 0;
		boolean is_negative = false;
		int start_index = 0;
		String formula = operands[0];
		for(int i = 0; i < formula.length(); ++i) {
			if(formula.charAt(i) != '+' && formula.charAt(i) != '-')
				continue;
			String part = formula.substring(start_index, i);	//수식 피연산자를 +-를 기준으로 part로 쪼갬
			
			int part_value = getOperandPartValue(part, symtab, littab, is_negative);	//각각 part의 값을 구함
			result += is_negative ? -part_value : part_value;	//+- 부호에 따라 전체값에 증감
			is_negative = (formula.charAt(i) == '-');
			start_index = i + 1;
			
		}
		String part = formula.substring(start_index);
		int part_value = getOperandPartValue(part, symtab, littab, is_negative);
		result += is_negative ? -part_value : part_value; 
		return result;
	}
	
	
	
	//피연산자의 전체값을 구하고 relative address 여부에 따라 locctr와의 차이를 계산
	public int getTextAddtion(SymbolTable symtab, LiteralTable littab) {
		int operandResult = getOperandsResult(symtab, littab);
		if(getFlag(TokenTable.pFlag) == TokenTable.pFlag) {
			operandResult = operandResult - (location + locIncrement);
			operandResult &= (1 << 12) - 1;
		}
		return operandResult;
	}
	
	
	
	//opcode, nixbpe, 피연산자나 주소 차이 등을 더해서 object code를 계산
	public Text makeObjectCode(SymbolTable symtab, LiteralTable littab) {
		if(byteSize <= 0)
			return null;
		
		Text object_text = new Text(location, byteSize * 2, instruction.opcode);
		object_text.addData(getTextAddtion(symtab, littab), 0);
		object_text.addNixbpe(nixbpe);
		return object_text;
	}
}
