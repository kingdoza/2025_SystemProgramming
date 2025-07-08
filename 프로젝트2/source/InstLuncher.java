package SP25_simulator;

// instruction에 따라 동작을 수행하는 메소드를 정의하는 클래스

public class InstLuncher {
    ResourceManager rMgr;
    VisualSimulator vsim;

    public InstLuncher(ResourceManager resourceManager, VisualSimulator visualSimulator) {
        this.rMgr = resourceManager;
        this.vsim = visualSimulator;
    }

    // instruction 별로 동작을 수행하는 메소드를 정의
    // ex) public void add(){...}
    
    //두 값을 비교하고 결과에 따라 SW 레지스터 세팅, COMP COMR TIX TIXR 에서 주로 사용
	private void compareAndSetStatusReg(int value1, int value2) {
		if(value1 < value2) {
			rMgr.setRegisterValue("SW", -1);	//작으면 -1 지정
		}
		else if(value1 > value2) {
			rMgr.setRegisterValue("SW", 1);		//크면 1 지정
		}
		else {
			rMgr.setRegisterValue("SW", 0);		//같으면 0 지정
		}
	}
	
	
	
	//2형식의 레지스터 관련 명령어의 연산을 수행
	private boolean processRegOperator(Command command) {
		String opString = Command.operators.get(command.getOperator()).name;
		int firstRegNum = (command.getData() >> 4) & 0xF;
		int secondRegNum = (command.getData() >> 0) & 0xF;
		
		if(opString.equals("CLEAR")) {	//CLEAR, 레지스터 값 초기화
			rMgr.setRegister(firstRegNum, 0);
		}
		else if(opString.equals("TIXR")) {	//TIXR, X 레지스터 1 증가하고 값 비교
			rMgr.setRegisterValue("X", rMgr.getRegisterValue("X") + 1);
			int xReg = rMgr.getRegisterValue("X");
			int otherReg = rMgr.getRegister(firstRegNum);
			compareAndSetStatusReg(xReg, otherReg);
		}
		else if(opString.equals("COMPR")) {	//COMPR, 두 레지스터의 값 비교
			int firstReg = rMgr.getRegister(firstRegNum);
			int secondReg = rMgr.getRegister(secondRegNum);
			compareAndSetStatusReg(firstReg, secondReg);
		}
		return true;
	}
	
	
	
	//명령어 정보에 따라 target address 설정하기
	private int setTargetAddresss(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		//e, p, b 주소 지정 방식에 따라 target address 지정
		if(command.hasFlag(Command.E_FLAG)) {
			targetAddr = command.getOperandValue(false);
		}
		else if(command.hasFlag(Command.P_FLAG)) {
			targetAddr = command.getOperandValue(true) + rMgr.getRegisterValue("PC");
		}
		else if(command.hasFlag(Command.B_FLAG)) {
			targetAddr = command.getOperandValue(false) + rMgr.getRegisterValue("B");
		}
		else {
			targetAddr = command.getOperandValue(true);
		}
		
		//x 플래그에 따라 x 레지스터 값 target address에 더하기
		if(command.hasFlag(Command.X_FLAG)) {
			targetAddr += rMgr.getRegisterValue("X");
		}
		rMgr.setRegisterValue("TA", targetAddr);
		return targetAddr;
	}
	
	
	
	//3, 4형식의 명령어의 연산을 수행
	private boolean processNormalOperator(Command command) {
		int targetAddr = setTargetAddresss(command);
		String opString = Command.operators.get(command.getOperator()).name;
		//실제 연산 실행
		if(opString.length() >= 3 && opString.substring(0, 2).equals("LD")) {	//LD*, 메모리->레지스터로 값 이동
			String reg = opString.substring(2);
			ld(command, reg);
		}
		else if(opString.length() >= 3 && opString.substring(0, 2).equals("ST")) {	//ST*, 레지스터->메모리로 값 이동
			String reg = opString.substring(2);
			st(command, reg);
		}
		else if(opString.equals("TIX")) {	//TIX, X 레지스터 1 증가하고 값 비교
			tix(command);
		}
		else if(opString.equals("COMP")) {	//COMP, A 레지스터의 값과 피연산자 값 비교
			comp(command);
		}
		else if(opString.equals("JEQ")) {	//SW == 0(직전 비교값이 같으면) 분기
			jeq(command);
		}
		else if(opString.equals("JGT")) {	//SW == 1(직전 비교값이 크면) 분기
			jgt(command);
		}
		else if(opString.equals("JLT")) {	//SW == 1(직전 비교값이 작으면) 분기
			jlt(command);
		}
		else if(opString.equals("J")) {	//해당 위치로 분기
			j(command);
			if(rMgr.getRegisterValue("PC") == 0)
				return false;
		}
		else if(opString.equals("JSUB")) {	//L 레지스터 리턴 주소 저장해두고 서브 루틴으로 분기
			jsub(command);
		}
		else if(opString.equals("RSUB")) {	//L 레지스터의 리턴 주소로 분기
			rsub(command);
		}
		else if(opString.equals("TD")) {	//test device, 이 프로젝트에서는 파일 스트림 생성
			td(command);
		}
		else if(opString.equals("RD")) {	//read device, 이 프로젝트에서는 파일 한 바이트 읽기
			rd(command);
		}
		else if(opString.equals("WD")) {	//write device, 이 프로젝트에서는 파일 한 바이트 쓰기
			wd(command);
		}
		return true;
	}
	
	
	
	//LD*
	private void ld(Command command, String reg) {
		int targetAddr = rMgr.getRegisterValue("TA");
		int loadValue = 0;
		if(!command.hasFlag(Command.N_FLAG) && command.hasFlag(Command.I_FLAG)) {
			loadValue = targetAddr;
		}
		else {
			int loadSize = reg.equals("CH") ? 1 : 3;
			loadValue = rMgr.getMemoryToInt(targetAddr, loadSize);
		}
		rMgr.setRegisterValue(reg, loadValue);
	}
	
	
	
	//ST*
	private void st(Command command, String reg) {
		int targetAddr = rMgr.getRegisterValue("TA");
		int regValue = rMgr.getRegisterValue(reg);
		int storeSize = reg.equals("CH") ? 1 : 3;
		rMgr.setMemoryFromInt(targetAddr, regValue, storeSize);
	}
	
	
	
	//TIX
	private void tix(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		int compValue = 0;
		if(!command.hasFlag(Command.N_FLAG) && command.hasFlag(Command.I_FLAG)) {
			compValue = targetAddr;
		}
		else {
			compValue = rMgr.getMemoryToInt(targetAddr, 3);
		}
		rMgr.setRegisterValue("X", rMgr.getRegisterValue("X") + 1);
		compareAndSetStatusReg(rMgr.getRegisterValue("X"), compValue);
	}
	
	
	
	//COMP
	private void comp(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		int compValue = 0;
		if(!command.hasFlag(Command.N_FLAG) && command.hasFlag(Command.I_FLAG)) {
			compValue = targetAddr;
		}
		else {
			compValue = rMgr.getMemoryToInt(targetAddr, 3);
		}
		compareAndSetStatusReg(rMgr.getRegisterValue("A"), compValue);
	}
	
	
	
	//JEQ
	private void jeq(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		if(rMgr.getRegisterValue("SW") != 0)
			return;
		rMgr.setRegisterValue("PC", targetAddr);
	}
	
	
	
	//JGT
	private void jgt(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		if(rMgr.getRegisterValue("SW") != 1)
			return;
		rMgr.setRegisterValue("PC", targetAddr);
	}
	
	
	
	//JLT
	private void jlt(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		if(rMgr.getRegisterValue("SW") != -1)
			return;
		rMgr.setRegisterValue("PC", targetAddr);
	}
	
	
	
	//J
	private void j(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		if(command.hasFlag(Command.N_FLAG) && !command.hasFlag(Command.I_FLAG)) {
			int memValue = rMgr.getMemoryToInt(targetAddr, 3);
			rMgr.setRegisterValue("PC", memValue);
		}
		else {
			rMgr.setRegisterValue("PC", targetAddr);
		}
	}
	
	
	
	//JSUB
	private void jsub(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		rMgr.setRegisterValue("L", rMgr.getRegisterValue("PC"));
		rMgr.setRegisterValue("PC", targetAddr);
	}
	
	
	
	//RSUB
	private void rsub(Command command) {
		rMgr.setRegisterValue("PC", rMgr.getRegisterValue("L"));
	}
	
	
	
	//TD
	private void td(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		int deviceNum = rMgr.getMemoryToInt(targetAddr, 1) & 0xFF;
		String device = String.format("%02X", deviceNum);
		rMgr.testDevice(device);
		rMgr.setRegisterValue("SW", 1);
		vsim.setUsingDevice(device);
	}
	
	
	
	//RD
	private void rd(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		int deviceNum = rMgr.getMemoryToInt(targetAddr, 1) & 0xFF;
		String device = String.format("%02X", deviceNum);
		byte dataRead = rMgr.readDevice(device);
		rMgr.setRegisterValue("CH", dataRead);
		vsim.setUsingDevice(device);
	}
	
	
	
	//WD
	private void wd(Command command) {
		int targetAddr = rMgr.getRegisterValue("TA");
		int deviceNum = rMgr.getMemoryToInt(targetAddr, 1) & 0xFF;
		String device = String.format("%02X", deviceNum);
		byte dataWrite = (byte)(rMgr.getRegisterValue("CH") & 0xFF);
		rMgr.writeDevice(device, dataWrite);
		vsim.setUsingDevice(device);
	}

	
	
	//명령어의 형식에 따라 연산 방식 지정, sicSimulator 에서 사용
	public boolean processOperator(Command command) {
		vsim.setUsingDevice(null);
		if(command.getLength() == 4) {
			return processRegOperator(command);
		}
		else {
			return processNormalOperator(command);
		}
	}
}