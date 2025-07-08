package SP25_simulator;

import java.awt.EventQueue;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap;

/**
 * VisualSimulator는 사용자와의 상호작용을 담당한다. 즉, 버튼 클릭등의 이벤트를 전달하고 그에 따른 결과값을 화면에 업데이트
 * 하는 역할을 수행한다.
 * 
 * 실제적인 작업은 SicSimulator에서 수행하도록 구현한다.
 */
public class VisualSimulator {
	//리소스 매니저, 시뮬레이터, 로더
	ResourceManager resourceManager = new ResourceManager();
	SicSimulator sicSimulator = new SicSimulator(resourceManager, this);
	SicLoader sicLoader = new SicLoader(resourceManager, sicSimulator);
	
	//오브젝트 코드 명령어와 위치 목록, 스크롤 목록
	DefaultListModel<String> commandsModel;
	ArrayList<Integer> commandLocList;
	JList<String> commandScrollList;
	
	//oneStep, allStep 버튼
	JButton stepInstallBtn;
	JButton allInstallBtn;
	
	//레지스터 이름 -> 레지스터 인덱스 매핑
	HashMap<String, Integer> registerField = new HashMap<String, Integer>();
	//레지스터 10진수 목록
	JTextField[] registerDecFields;
	//레지스터 16진수 목록
	JTextField[] registerHexFields;
	
	//헤더 정보 : 프로그램 이름, 시작 위치, 길이
	JTextField programNameField;
	JTextField startAddrField;
    JTextField lengthField;
    
    //end record 정보
    JTextField instAddressField;
    
    //"Start address in memory" 필드
    JTextField memoryStartField;
    
    //"Target address" 필드
    JTextField targetAddrField;
    
    //명령어 로그 관련 목록 및 스크롤
    DefaultListModel<String> logModel;
    JList<String> logList;
    
    //사용중인 장치 필드
    JTextField deviceField;
    
    
	
    //레지스터 인덱스 매핑 초기화 및 레지스터 배열 초기화
	public VisualSimulator() {
		registerField.put("A", 0);
		registerField.put("X", 1);
		registerField.put("L", 2);
		registerField.put("B", 3);
		registerField.put("S", 4);
		registerField.put("T", 5);
		registerField.put("F", 6);
		registerField.put("PC", 7);
		registerField.put("SW", 8);
		
		registerDecFields = new JTextField[registerField.size()];
		registerHexFields = new JTextField[registerField.size()];
		commandLocList = new ArrayList<>();
	}

	/**
	 * 프로그램 로드 명령을 전달한다.
	 */
	public void load(File program) {
		// ...
		sicLoader.load(program);
		sicSimulator.load(program);
		showAllRegisters();
		instAddressField.setText(String.format("%06X", sicSimulator.loadLocation));
		memoryStartField.setText(String.format("%X", sicSimulator.loadLocation));
	};

	/**
	 * 하나의 명령어만 수행할 것을 SicSimulator에 요청한다.
	 */
	public void oneStep() {
		sicSimulator.oneStep();
	};

	/**
	 * 남아있는 모든 명령어를 수행할 것을 SicSimulator에 요청한다.
	 */
	public void allStep() {
		sicSimulator.allStep();
	};

	/**
	 * 화면을 최신값으로 갱신하는 역할을 수행한다.
	 */
	public void update() {

	};
	
	

	//GUI 요소와 프레임 생성
	public static void main(String[] args) {
		VisualSimulator visualSimulator = new VisualSimulator();
		
		//타이틀 프레임
		JFrame frame = new JFrame("SIC/XE Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(560, 765);
        frame.setLocationRelativeTo(null);
        frame.setLayout(null); // 절대 좌표 배치

        //파일 이름 레이블 생성
        JLabel label = new JLabel("FileName :");
        label.setBounds(15, 10, 60, 25);
        frame.add(label);

        //파일 이름 텍스트 필드 생성
        JTextField textField = new JTextField();
        textField.setBounds(80, 10, 200, 25);
        textField.setEnabled(false);
        frame.add(textField);

        //파일 탐색 버튼 생성
        JButton button = new JButton("open");
        button.setBounds(290, 10, 80, 25);
        frame.add(button);

        //파일 탐색 버튼 클릭 시 동작
        button.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(frame);  //파일 다이얼로그 열기
            
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                textField.setText(selectedFile.getAbsolutePath());
                
                visualSimulator.load(selectedFile);
                visualSimulator.appendCommands();
                visualSimulator.stepInstallBtn.setEnabled(true);
                visualSimulator.allInstallBtn.setEnabled(true);
            }
        });
        
        //여러 하위 프레임들 추가
        frame.add(visualSimulator.makeEndPanel());
        frame.add(visualSimulator.makeHeaderPanel());
        frame.add(visualSimulator.makeRegisterPanel());	
        frame.add(visualSimulator.makeAddressPanel());
        frame.add(visualSimulator.makeInstList(), BorderLayout.WEST);
        frame.add(visualSimulator.makeControlButtons());
        
        //명령어 로그 프레임 생성
        JLabel logLabel = new JLabel("Log (명령어 수행 관련)");
        logLabel.setBounds(17, 535, 150, 25);
        frame.add(logLabel);
        visualSimulator.logModel = new DefaultListModel<>();
        visualSimulator.logList = new JList<>(visualSimulator.logModel);
        visualSimulator.logList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane logListScrollPane = new JScrollPane(visualSimulator.logList);
        logListScrollPane.setBounds(17, 560, 510, 150);
        frame.add(logListScrollPane, BorderLayout.WEST);
            
        //최상단의 GUI 프레임 표시
        frame.setVisible(true);
	}
	
	
	
	//명령어 코드 목록에 로드된 명령어 코드 추가
	private void appendCommands() {
		ArrayList<Command> commandList = sicSimulator.parseCommands(resourceManager.getMemory(0, resourceManager.offset));
		SwingUtilities.invokeLater(() -> {
	        for (Command command : commandList) {
	            commandsModel.addElement(command.toString());
	            commandLocList.add(command.getLocation());
	        }
	    });
	}
	
	
	
	//헤더 레코드 영역 패널 생성
	private JPanel makeHeaderPanel() {
		//그리드 형식의 패널 생성
		JPanel hPanel = new JPanel(new GridBagLayout());
        hPanel.setBorder(BorderFactory.createTitledBorder("H (Header Record)"));
        GridBagConstraints hGbc = new GridBagConstraints();
        hGbc.insets = new Insets(5, -5, 5, 5);
        hGbc.anchor = GridBagConstraints.WEST;

        //레이블 생성
        hGbc.gridx = 0; hGbc.gridy = 0;
        hPanel.add(new JLabel("Program name :"), hGbc);

        //텍스트 필드 생성
        hGbc.gridx = 1; hGbc.gridy = 0; hGbc.anchor = GridBagConstraints.EAST;
        programNameField = new JTextField(8);
        programNameField.setPreferredSize(new Dimension(programNameField.getPreferredSize().width, 25));
        programNameField.setEditable(false);
        hPanel.add(programNameField, hGbc);

        //레이블 생성
        hGbc.gridx = 0; hGbc.gridy = 1; hGbc.anchor = GridBagConstraints.WEST;
        hPanel.add(new JLabel("<html>Start Address of<br>Object Program :</html>"), hGbc);

        //텍스트 필드 생성
        hGbc.gridx = 1; hGbc.gridy = 1; hGbc.anchor = GridBagConstraints.EAST;
        startAddrField = new JTextField(8);
        startAddrField.setPreferredSize(new Dimension(startAddrField.getPreferredSize().width, 25));
        startAddrField.setEditable(false);
        hPanel.add(startAddrField, hGbc);

        //레이블 생성
        hGbc.gridx = 0; hGbc.gridy = 2; hGbc.anchor = GridBagConstraints.WEST;
        hPanel.add(new JLabel("Length of Program :"), hGbc);

        //텍스트 필드 생성
        hGbc.gridx = 1; hGbc.gridy = 2; hGbc.anchor = GridBagConstraints.EAST;
        lengthField = new JTextField(7);
        lengthField.setPreferredSize(new Dimension(lengthField.getPreferredSize().width, 25));
        lengthField.setEditable(false);
        hPanel.add(lengthField, hGbc);
     
        hPanel.setBounds(15, 50, 250, 150);
        return hPanel;
	}
	
	
	
	//end 레코드 영역 패널 생성
	private JPanel makeEndPanel() {
		//그리드 형식의 패널 생성
		JPanel ePanel = new JPanel(new GridBagLayout());
        ePanel.setBorder(BorderFactory.createTitledBorder("E (End Record)"));
        GridBagConstraints eGbc = new GridBagConstraints();
        eGbc.insets = new Insets(5, 5, 5, 5);
        eGbc.anchor = GridBagConstraints.WEST;

        //레이블 생성
        eGbc.gridx = 0; eGbc.gridy = 0;
        ePanel.add(new JLabel("<html>Address of<br>First Instruction<br>in Object Program : </html>"), eGbc);

        //텍스트 필드 생성
        eGbc.gridx = 1; eGbc.gridy = 0; eGbc.anchor = GridBagConstraints.EAST;
        instAddressField = new JTextField(8);
        instAddressField.setPreferredSize(new Dimension(instAddressField.getPreferredSize().width, 25));
        instAddressField.setEditable(false);
        ePanel.add(instAddressField, eGbc);
        
        ePanel.setBounds(280, 50, 250, 100);
        return ePanel;
	}
	
	
	
	private JPanel makeRegisterPanel() {
		//그리드 형식의 패널 생성
		JPanel rPanel = new JPanel(new GridBagLayout());
        rPanel.setBorder(BorderFactory.createTitledBorder("Register"));
        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.insets = new Insets(2, 4, 2, 4);
        rGbc.anchor = GridBagConstraints.WEST;
        
        //타이틀 레이블 생성
        rGbc.gridx = 1; rGbc.gridy = 0;
        rPanel.add(new JLabel("Dec"), rGbc);
        rGbc.gridx = 2; rGbc.gridy = 0;
        rPanel.add(new JLabel("Hex"), rGbc);
        
        int yPos = 1;
        //<레지스터 번호, 레지스터 이름> 페어로 레지스터별 영역 생성
        for (Map.Entry<Integer, String> entry : resourceManager.registerNum.entrySet()) {
        	//레이블 생성
        	rGbc.gridx = 0; rGbc.gridy = yPos; rGbc.anchor = GridBagConstraints.EAST;
        	JLabel regLabel = new JLabel(String.format("%s (#%d)", entry.getValue(), entry.getKey()));
        	rPanel.add(regLabel, rGbc);
        	
        	//10진수 텍스트 필드 생성
        	rGbc.gridx = 1; rGbc.gridy = yPos; rGbc.anchor = GridBagConstraints.EAST;
            JTextField decField = new JTextField(6);
            decField.setPreferredSize(new Dimension(decField.getPreferredSize().width, 25));
            decField.setEditable(false);
            rPanel.add(decField, rGbc);
            registerDecFields[yPos - 1] = decField;
            
            //16진수 텍스트 필드 생성
            rGbc.gridx = 2; rGbc.gridy = yPos; rGbc.anchor = GridBagConstraints.EAST;
            JTextField hexField = new JTextField(6);
            hexField.setPreferredSize(new Dimension(hexField.getPreferredSize().width, 25));
            hexField.setEditable(false);
            rPanel.add(hexField, rGbc);
            registerHexFields[yPos - 1] = hexField;
            yPos++;
        }
        
        rPanel.setBounds(15, 210, 250, 320);
        return rPanel;
	}
	
	
	
	private JPanel makeAddressPanel() {
		//그리드 형식의 패널 생성
		JPanel aPanel = new JPanel(new GridBagLayout());
        GridBagConstraints aGbc = new GridBagConstraints();
        aGbc.insets = new Insets(2, 0, 2, 0);
        aGbc.anchor = GridBagConstraints.WEST;

        //레이블 생성
        aGbc.gridx = 0; aGbc.gridy = 0; aGbc.anchor = GridBagConstraints.WEST;
        aPanel.add(new JLabel("Start address in memory :"), aGbc);

        //텍스트 필드 생성
        aGbc.gridx = 1; aGbc.gridy = 0; aGbc.anchor = GridBagConstraints.EAST;
        memoryStartField = new JTextField(8);
        memoryStartField.setPreferredSize(new Dimension(startAddrField.getPreferredSize().width, 25));
        memoryStartField.setEditable(false);
        aPanel.add(memoryStartField, aGbc);

        //레이블 생성
        aGbc.gridx = 0; aGbc.gridy = 1; aGbc.anchor = GridBagConstraints.WEST;
        aPanel.add(new JLabel("Target Address :"), aGbc);

        //텍스트 필드 생성
        aGbc.gridx = 1; aGbc.gridy = 1; aGbc.anchor = GridBagConstraints.EAST;
        targetAddrField = new JTextField(8);
        targetAddrField.setPreferredSize(new Dimension(targetAddrField.getPreferredSize().width, 25));
        targetAddrField.setEditable(false);
        aPanel.add(targetAddrField, aGbc);
        
        //스크롤 목록의 레이블 생성
        aGbc.gridx = 0; aGbc.gridy = 2; aGbc.anchor = GridBagConstraints.WEST;
        aPanel.add(new JLabel("Instructions :"), aGbc);
        
        aPanel.setBounds(280, 150, 255, 90);
        return aPanel;
	}
	
	
	
	private JScrollPane makeInstList() {
		//그리드 형식의 패널 생성
        GridBagConstraints iGbc = new GridBagConstraints();
        iGbc.insets = new Insets(0, 0, 2, 0);
        iGbc.anchor = GridBagConstraints.WEST;
        
        //명령어 코드 스크롤 목록 생성
        commandsModel = new DefaultListModel<>();
        commandScrollList = new JList<>(commandsModel);
        commandScrollList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(commandScrollList);
        
        scrollPane.setBounds(285, 240, 130, 290);
        return scrollPane;
	}
	
	
	
	private JPanel makeControlButtons() {
		//그리드 형식의 패널 생성
		JPanel ePanel = new JPanel(new GridBagLayout());
        GridBagConstraints eGbc = new GridBagConstraints();
        eGbc.insets = new Insets(3, 0, 3, 0);
        eGbc.anchor = GridBagConstraints.CENTER;

        //레이블 생성
        eGbc.gridx = 0; eGbc.gridy = 0;
        ePanel.add(new JLabel("사용중인 장치"), eGbc);

        //텍스트 필드 생성
        eGbc.gridx = 0; eGbc.gridy = 1; eGbc.anchor = GridBagConstraints.CENTER;
        eGbc.insets = new Insets(0, 0, 135, 0);
        deviceField = new JTextField(6);
        deviceField.setHorizontalAlignment(JTextField.CENTER);
        deviceField.setPreferredSize(new Dimension(deviceField.getPreferredSize().width, 25));
        deviceField.setEditable(false);
        ePanel.add(deviceField, eGbc);
        
        //oneStep 버튼 생성
        eGbc.gridx = 0; eGbc.gridy = 2; eGbc.anchor = GridBagConstraints.CENTER;
        eGbc.insets = new Insets(5, 0, 5, 0);
        stepInstallBtn = new JButton("실행(1step)");
        stepInstallBtn.setPreferredSize(new Dimension(100, 25));
        stepInstallBtn.setEnabled(false);
        ePanel.add(stepInstallBtn, eGbc);
        
        //oneStep 버튼 클릭 이벤트
        stepInstallBtn.addActionListener(e -> {
            oneStep();
        });
        
        //allStep 버튼 생성
        eGbc.gridx = 0; eGbc.gridy = 3; eGbc.anchor = GridBagConstraints.CENTER;
        allInstallBtn = new JButton("실행(all)");
        allInstallBtn.setPreferredSize(new Dimension(100, 25));
        allInstallBtn.setEnabled(false);
        ePanel.add(allInstallBtn, eGbc);
        
        //allStep 버튼 클릭 이벤트
        allInstallBtn.addActionListener(e -> {
            allStep();
        });
        
        //종료 버튼 생성
        eGbc.gridx = 0; eGbc.gridy = 4; eGbc.anchor = GridBagConstraints.CENTER;
        JButton exitBtn = new JButton("종료");
        exitBtn.setPreferredSize(new Dimension(100, 25));
        ePanel.add(exitBtn, eGbc);
        
        //종료 버튼 클릭 이벤트
        exitBtn.addActionListener(e -> {
            System.exit(0);
        });
        
        ePanel.setBounds(420, 235, 110, 310);
        return ePanel;
	}
	
	
	
	//리소스 매니저의 모든 레지스타값 불러와서 GUI에 업데이트
	public void showAllRegisters() {
		for (String register : registerField.keySet()) {
			showRegisterValue(register, resourceManager.registers.get(register));
		}
	}
	
	
	
	//지정한 레지스타의 값만 불러와서 GUI에 업데이트
	public void showRegisterValue(String name, int value) {
		int regidx = registerField.get(name);
		if(!name.equals("F") && !name.equals("SW"))
			registerDecFields[regidx].setText(Integer.toString(value));
		registerHexFields[regidx].setText(String.format("%06X", value));
	}
	
	
	
	//프로그램 헤더 정보 불러와서 GUI에 업데이트
	public void showHeader(Program program) {
		programNameField.setText(program.getName());
		startAddrField.setText(String.format("%06X", program.getStart()));
		lengthField.setText(String.format("%06X", program.getLength()));
	}
	
	
	
	//target address 업데이트
	public void showTargetAddr() {
		targetAddrField.setText(String.format("%06X", resourceManager.getRegisterValue("TA")));
	}
	
	
	
	//명령어 실행 로그 추가
	public void addOperatorLog(String operator) {
		logModel.addElement(operator);
		logList.ensureIndexIsVisible(logModel.size() - 1);
	}
	
	
	
	//실행중인 명령어 코드를 스크롤 목록에서 선택 표시
	public void selectCommand(Command command) {
		for(int i = 0; i < commandsModel.size(); ++i) {
			if(commandLocList.get(i) == command.getLocation()) {
				commandScrollList.setSelectedIndex(i);
				commandScrollList.ensureIndexIsVisible(i);
			}
		}
	}
	
	
	
	//사용중인 장치 정보 업데이트
	public void setUsingDevice(String device) {
		if(device == null)
			deviceField.setText("");
		else
			deviceField.setText(device);
	}
}
