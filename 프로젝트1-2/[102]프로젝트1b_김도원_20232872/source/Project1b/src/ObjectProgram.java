import java.io.PrintStream;
import java.util.*;



/*Section별 헤더 정보, object code 목록, modification 목록, EXTREF EXTDEF 정보 등을 저장하는 클래스다. 
 *Section별로 하나씩 할당된다.
 */
public class ObjectProgram {
	//메인 section 여부
	boolean isMain;
	//헤더 정보
	Header header;
	//object code 리스트
	ArrayList<Text> textList;
	//modification record 리스트
	ArrayList<Modification> modList;
	//extref 레이블 리스트
	ArrayList<String> extrefList;
	//extdef 레이블과 위치의 리스트
	ArrayList<Map.Entry<String, Integer>> extdefList;
	
	
	
	//생성자, 멤버변수 초기화
	public ObjectProgram() {
		textList = new ArrayList<Text>();
		modList = new ArrayList<Modification>();
		header = new Header();
		extrefList = new ArrayList<String>();
		extdefList = new ArrayList<>();
	}
	
	
	
	//생성자, section 이름과 프로그램 시작 위치를 저장
	public ObjectProgram(String section, int start) {
		this();
		header.name = section;
		header.start = start;
		this.isMain = false;
	}
	
	
	//생성자, 해당 section의 메인 여부를 저장
	public ObjectProgram(String section, int start, boolean isMain) {
		this(section, start);
		this.isMain = isMain;
	}
	
	
	
	//object code 리스트에 object code 추가
	public void addObjectText(Text text) {
		if(text == null)
			return;
		textList.add(text);
	}
	
	
	
	//modification record 리스트에 modification record 추가
	public void appendMods(ArrayList<Modification> mods) {
		if(mods == null)
			return;
		for(Modification mod : mods) {
			modList.add(mod);
		}
	}
	
	
	
	//object code의 위치를 기준으로 object code 리스트 오름차순 정렬
	public void sortTexts() {
		Iterator<Text> iterator = textList.iterator();
		while (iterator.hasNext()) {
		    if (iterator.next() == null) {
		        iterator.remove();
		    }
		}
		Collections.sort(textList);
	}
	
	
	
	//object program의 EXTREF 레코드 문자열
	private String getExtrefString() {
		StringBuilder sb = new StringBuilder();
		for(String extref : extrefList) {
			sb.append(String.format("%-6s", extref));
		}
		return sb.toString();
	}
	
	
	
	//object program의 EXTDEF 레코드 문자열
	private String getExtdefString() {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> entry : extdefList) {
		    String extdef = entry.getKey();
		    Integer location = entry.getValue();
		    sb.append(String.format("%-6s%06X", extdef, location));
		}
		return sb.toString();
	}
	
	
	
	//EXTDEF 레이블을 심볼 테이블에서 찾아서 주소를 EXTDEF 리스트에 저장
	public void setExtdefLocation(SymbolTable symtab) {
		for (Map.Entry<String, Integer> entry : extdefList) {
		    String extdef = entry.getKey();
		    entry.setValue(symtab.searchSymbol(extdef));
		}
	}
	
	
	
	//object program의 맨 끝 줄의 레코드 문자열
	private String getEndString() {
		if(isMain)
			return String.format("%06X", header.start);
		return "";
	}
	
	
	
	//object program의 Text 영역 문자열
	private String[] getTextStrings() {
		ArrayList<String> text_strings = new ArrayList<String>();
		
		int locctr = 0;
		int line_bytes = 0;
		boolean line_break = true;
		StringBuilder sb = null;
		
		for(Text text : textList) {
			int text_bytes = text.length / 2;
			if(line_bytes + text_bytes > 30 || locctr != text.location) {	//줄 길이가 30이 넘거나 다음 locctr에 object code가 없다면 줄 넘김
				locctr = text.location;
				line_break = true;
			}
			if(line_break) {
				if(sb != null) {
					sb.insert(6, String.format("%02X", line_bytes));	//줄 길이 삽입
					text_strings.add(sb.toString());
				}
				sb = new StringBuilder(String.format("%06X", text.location));	//줄 시작 위치 저장
				line_bytes = 0;
				line_break = false;
			}
			sb.append(text);
			line_bytes += text_bytes;
			locctr += text_bytes;
		}
		
		if(sb != null) {
			sb.insert(6, String.format("%02X", line_bytes));
			text_strings.add(sb.toString());
		}
		
		return text_strings.toArray(new String[0]);
	}
	
	
	
	//헤더, 텍스트, modification record 등으로 이루어진 object program을 printStream으로 작성
	public void print(PrintStream ps) {
		ps.println("H" + header);	//헤더 정보 출력
		String extdef_string = getExtdefString();
		if (extdef_string != null && !extdef_string.isEmpty()) {
			ps.println("D" + extdef_string);	//EXTDEF 정보 출력
		}
		String extref_string = getExtrefString();
		if (extref_string != null && !extref_string.isEmpty()) {
			ps.println("R" + extref_string);	//EXTREF 정보 출력
		}
		for(String text_string : getTextStrings()) {
			ps.println("T" + text_string);	//텍스트 영역 출력
		}
		for(Modification mod : modList) {
			ps.println("M" + mod);	//Modification record 출력
		}
		ps.println("E" + getEndString());	//End 정보 출력
	}
	
	
	
	//프로그램의 길이 setter 함수
	public void setLength(int length) {
		header.length = length;
	}
	
	
	
	//EXTDEF 리스트에 레이블과 위치 추가
	public void addExtdef(String extdef, int location) {
		extdefList.add(new AbstractMap.SimpleEntry<>(extdef, location));
	}
	
	
	
	//EXTREF 리스트에 레이블 추가
	public void addExtref(String extref) {
		extrefList.add(extref);
	}
}



/*토큰이랑 리터럴 테이블에서 변환된 object code 클래스다. */
class Text implements Comparable<Text> {
	//object code data
	int data;
	//object code 16진법 변환 시의 길이
	int length;
	//object code의 위치
	int location;
	
	
	
	//생성자, 위치, 길이를 저장하고 object code(data)를 opcode로 초기화
	public Text(int location, int length, byte opcode) {
		this.data = 0;
		this.location = location;
		this.length = length;
		if((opcode & 0xFF) != 0xFF) {
			addData(opcode, length - 2);
		}
	}
	
	
	
	//16진법으로 오른쪽에서 shifts번째 자리를 addition으로 세팅
	public void addData(int addition, int shifts) {
		data |= addition << (shifts * 4);
	}
	
	
	
	//object code에 nixbpe 값 세팅
	public void addNixbpe(char nixbpe) {
		if(length == 6 || length == 8)
			addData(nixbpe, length - 3);
	}
	
	
	
	//object code에 피연산자 값 혹은 주소 차이 값 세팅
	public void addOperandValue(int operandValue) {
		int mask = (1 << ((length - 3) * 4)) - 1;
	    int lower_bits = operandValue & (int)mask;
		addData(lower_bits, 0);
	}
	
	
	
	//object code를 바꿈
	public void setData(int new_data) {
		data = new_data;
	}
	
	
	
	//위치 기준 정렬을 위한 비교 함수
	@Override
    public int compareTo(Text other) {
        return Integer.compare(this.location, other.location);
    }
	
	
	
	//저장된 16진법 길이만큼 object code를 문자열로 변환
	@Override
	public String toString() {
		long mask = (1L << (length * 4)) - 1;
	    int lower_bits = data & (int)mask;
	    return String.format("%0" + length + "X", lower_bits);
	}
}





/*Section의 헤더 작성을 위한 프로그램의 시작 위치, 길이, 이름 등을 저장하는 클래스다.*/
class Header {
	int start;
	int length;
	String name;
	
	
	
	//기본 생성자
	public Header() { }
	
	
	
	//object program의 헤더 정보 문자열
	@Override
	public String toString() {
		return String.format("%-6s%06X%06X", name, start, length);
	}
}



/*Section의 Modification record 작성을 위한 수정 위치, 길이, 연산자(+,-), 대상 심 심볼 등을 저장하는 클래스다.*/
class Modification {
	int location;
	int length;
	char operator;
	String symbol;
	
	
	
	//생성자, 수정위치, 수정길이, 연산자, 대상 심볼 저장
	public Modification(int location, int length, char operator, String symbol) {
		this.location = location;
		this.length = length;
		this.operator = operator;
		this.symbol = symbol;
	}
	
	
	
	//object program의 modification record 정보 문자열
	@Override
	public String toString() {
		return String.format("%06X%02X%c%s", location, length, operator, symbol);
	}
}