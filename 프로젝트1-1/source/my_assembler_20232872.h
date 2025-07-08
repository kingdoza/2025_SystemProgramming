/*
 * my_assembler �Լ��� ���� ���� ���� �� ��ũ�θ� ��� �ִ� ��� �����̴�.
 *
 */
#define MAX_INST 256
#define MAX_LINES 5000
#define MAX_OPERAND 3

 /*
  * instruction ��� ���Ϸ� ���� ������ �޾ƿͼ� �����ϴ� ����ü �����̴�.
  * ���� ���� �ϳ��� instruction�� �����Ѵ�.
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
 * ����� �� �ҽ��ڵ带 �Է¹޴� ���̺��̴�. ���� ������ ������ �� �ִ�.
 */
char* input_data[MAX_LINES];
static int line_num;

/*
 * ����� �� �ҽ��ڵ带 ��ū������ �����ϱ� ���� ����ü �����̴�.
 * operator�� renaming�� ����Ѵ�.
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
 * �ɺ��� �����ϴ� ����ü�̴�.
 * �ɺ� ���̺��� �ɺ� �̸�, �ɺ��� ��ġ�� �����ȴ�.
 * ���� ������ ��� ����
 */
typedef struct _symbol
{
	char symbol[10];
	int addr;
} symbol;

/*
* ���ͷ��� �����ϴ� ����ü�̴�.
* ���ͷ� ���̺��� ���ͷ��� �̸�, ���ͷ��� ��ġ�� �����ȴ�.
* ���� ������ ��� ����
*/
typedef struct _literal {
	char* literal;
	int addr;
} literal;

symbol sym_table[MAX_LINES];
literal literal_table[MAX_LINES];


/**
 * ������Ʈ �ڵ� ��ü�� ���� ������ ��� ����ü�̴�.
 * Header Record, Define Recode,
 * Modification Record � ���� ������ ��� �����ϰ� �־�� �Ѵ�. ��
 * ����ü ���� �ϳ������� object code�� ����� �ۼ��� �� �ֵ��� ����ü�� ����
 * �����ؾ� �Ѵ�.
 */

//16���� �ڵ� ����ü
typedef struct _binary {
	unsigned int code;	//16���� �ڵ�
	size_t len;			//����Ʈ ��
	int start;			//���� ��ġ
} binary;

//modification record ����ü
typedef struct _mod_record {
	unsigned int pos;	//���� ��ġ
	unsigned int bytes;	//���� ����
	int neg;			//���� ����
	char* label;		//���� ��� ���̺�
	int idx;			//mod record�� ������� ����
} mod_record;

//�� ���α׷��� ������Ʈ �ڵ� ����
typedef struct _object_code {
	char* name;					//���α׷� �̸�
	int start;					//���� ��ġ
	int length;					//���α׷� ����
	char** defs;				//extdef ��� ���̺�
	size_t deflen;				//extdef ��� ���̺� ����
	char** refs;				//extref ��� ���̺�
	size_t reflen;				//extref ��� ���̺� ����
	binary bins[MAX_LINES];		//16���� �ڵ� �迭
	size_t bincnt;				//16���� �ڵ� ����
	mod_record mods[MAX_LINES];	//modification record �迭
	size_t modcnt;				//modification record ����
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