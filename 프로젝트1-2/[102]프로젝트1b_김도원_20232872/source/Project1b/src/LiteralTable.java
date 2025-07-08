import java.io.PrintStream;
import java.util.ArrayList;


public class LiteralTable {
    ArrayList<String> literalList;
    ArrayList<Integer> locationList;
    //리터얼의 길이 저장
    ArrayList<Integer> lengthList;
    //리터럴이 LTORG나 프로그램 끝을 만나 ㅊ
    ArrayList<Boolean> placementFlags;
    //리터럴의 원본 저장
    ArrayList<String> originalList;
    
    
    
    public LiteralTable() {
    	originalList = new ArrayList<String>();
    	literalList = new ArrayList<String>();
    	locationList = new ArrayList<Integer>();
    	lengthList = new ArrayList<Integer>();
    	placementFlags = new ArrayList<Boolean>();
	}
    
    

    public void putLiteral(String literal, int location) {
    	originalList.add(literal);
    	literalList.add(getLiteralString(literal));
    	locationList.add(location);
    	lengthList.add(getLiteralLength(literal));
    	placementFlags.add(false);
    }
    
    
    
    //제어문이 LTORG나 프로그램 끝을 만나서 object code로 안 변환된 리터럴들을 전부 변환
    public int ltorg(int location) {
    	for(int i = 0; i < literalList.size(); ++i) {
    		if(placementFlags.get(i) == false) {	//아직 배치안된 리터럴들 전부 찾아서 배치
    			String original = originalList.get(i);
    			String literal = literalList.get(i);
    			int length = lengthList.get(i);
    			while (literalList.contains(literal)) {	//배치된 리터럴을 기존 임시 리스트에서 중복 제거
    				int index = literalList.indexOf(literal);
    				originalList.remove(index);
    				literalList.remove(literal);
    				locationList.remove(index);
    				lengthList.remove(index);
    				placementFlags.remove(index);
    			}
    			originalList.add(original);
    			literalList.add(literal);
    			locationList.add(location);
    			lengthList.add(length);
    			placementFlags.add(true);
    			location += length / 2;
    		}
    	}
    	return location;
    }
    
    

    //리터럴의 바이트 수 반환
    public static int getLiteralLength(String literal) {
    	int start = literal.indexOf('\'');
        int end = literal.lastIndexOf('\'');

        if (start == -1 || end == -1 || start >= end) {
            return 0;
        }
        int length = (end - start - 1) * 2;
        length = (literal.charAt(start - 1) == 'X') ? length / 2 : length;
        return length;
    }
    
    
    
    //따옴표(')로 둘러싸여진 리터럴의 데이터 추출(ex. =C'EOF' -> EOF, =X'05' -> 05)
    public static String getLiteralString(String literal) {
    	int start = literal.indexOf('\'');
        int end = literal.lastIndexOf('\'');

        if (start == -1 || end == -1 || start >= end) {
            return null;
        }
        String extracted = literal.substring(start + 1, end);
        return extracted;
    }
    
    
    
    //리터럴을 object code로 변환
    public static int getLiteralCode(String literal) {;
		try {
			int byte_data = Integer.parseInt(literal, 16);
			return byte_data;
		} catch(NumberFormatException e) {
			StringBuilder sb = new StringBuilder();
			for (char c : literal.toCharArray()) {
			    sb.append(String.format("%02X", (int)c));
			}
			return Integer.parseUnsignedInt(sb.toString(), 16);
		}
	}
    
    
    
    //리터럴 테이블의 n번째 리터럴을 Text(object code)로 변환
    public Text getCodeText(int index) {
    	int literal_code = getLiteralCode(literalList.get(index));
    	Text code_text = new Text(locationList.get(index), lengthList.get(index), (byte)0xFF);
    	code_text.addData(literal_code, 0);
    	return code_text;
    }
	
    
    
    //리터럴 테이블에서 리터럴 검색, 없으면 -1 반환
    public int searchLiteral(String literal) {
		//...
		for(int i = 0; i < literalList.size(); ++i) {
			if(literalList.get(i).equals(literal))
				return locationList.get(i);
		}
		return -1;
	}
    
    
    
    //printStream(System.out or file)으로 리터럴 테이블의 레코드 정보를 출력
    public void print(PrintStream ps) {
    	for(int i = 0; i < literalList.size(); ++i) {
    		ps.printf("%s\t\t0x%04X\n", originalList.get(i), locationList.get(i));
    	}
	}
    // 필요 메서드 추가 구현
}