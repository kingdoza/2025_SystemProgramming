/*
 * my_assembler 함수를 위한 변수 선언 및 매크로를 담고 있는 헤더 파일이다.
 *
 */
#define MAX_INST 256
#define MAX_LINES 5000
#define MAX_OPERAND 3

 /*
  * instruction 목록 파일로 부터 정보를 받아와서 생성하는 구조체 변수이다.
  * 라인 별로 하나의 instruction을 저장한다.
  */
typedef struct _inst
{
	char str[10];
	unsigned char op;
	int format;
	int ops;
} inst;

inst* inst_table[MAX_INST];
int inst_index;

/*
 * 어셈블리 할 소스코드를 입력받는 테이블이다. 라인 단위로 관리할 수 있다.
 */
char* input_data[MAX_LINES];
static int line_num;

/*
 * 어셈블리 할 소스코드를 토큰단위로 관리하기 위한 구조체 변수이다.
 * operator는 renaming을 허용한다.
 */
typedef struct _token
{
	char* label;
	char* opt;
	char* operand[MAX_OPERAND];
	char comment[100];
	char nixbpe;
} token;

token* token_table[MAX_LINES];
static int token_line;

/*
 * 심볼을 관리하는 구조체이다.
 * 심볼 테이블은 심볼 이름, 심볼의 위치로 구성된다.
 * 추후 과제에 사용 예정
 */
typedef struct _symbol
{
	char symbol[10];
	int addr;
} symbol;

/*
* 리터럴을 관리하는 구조체이다.
* 리터럴 테이블은 리터럴의 이름, 리터럴의 위치로 구성된다.
* 추후 과제에 사용 예정
*/
typedef struct _literal {
	char* literal;
	int addr;
} literal;

symbol sym_table[MAX_LINES];
literal literal_table[MAX_LINES];


/**
 * 오브젝트 코드 전체에 대한 정보를 담는 구조체이다.
 * Header Record, Define Recode,
 * Modification Record 등에 대한 정보를 모두 포함하고 있어야 한다. 이
 * 구조체 변수 하나만으로 object code를 충분히 작성할 수 있도록 구조체를 직접
 * 정의해야 한다.
 */

//16진수 코드 구조체
typedef struct _binary {
	unsigned int code;	//16진수 코드
	size_t len;			//바이트 수
	int start;			//시작 위치
} binary;

//modification record 구조체
typedef struct _mod_record {
	unsigned int pos;	//수정 위치
	unsigned int bytes;	//수정 길이
	int neg;			//음수 여부
	char* label;		//연산 대상 레이블
	int idx;			//mod record가 만들어진 순서
} mod_record;

//각 프로그램별 오브젝트 코드 정보
typedef struct _object_code {
	char* name;					//프로그램 이름
	int start;					//시작 위치
	int length;					//프로그램 길이
	char** defs;				//extdef 대상 레이블
	size_t deflen;				//extdef 대상 레이블 개수
	char** refs;				//extref 대상 레이블
	size_t reflen;				//extref 대상 레이블 개수
	binary bins[MAX_LINES];		//16진수 코드 배열
	size_t bincnt;				//16진수 코드 개수
	mod_record mods[MAX_LINES];	//modification record 배열
	size_t modcnt;				//modification record 개수
	/* add fields */
} object_code;


static int locctr;
//--------------

static char* input_file;
static char* output_file;
int init_my_assembler(void);
int init_inst_file(char* inst_file);
int init_input_file(char* input_file);
int token_parsing(char* str);
int search_opcode(char* str);
static int assem_pass1(void);
void make_opcode_output(char* file_name);
void make_literaltab_output(char* filename);
void make_symtab_output(char* file_name);
static int assem_pass2(void);
void make_objectcode_output(char* file_name);