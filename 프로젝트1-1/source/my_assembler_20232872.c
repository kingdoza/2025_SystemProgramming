/*
 * 파일명 : my_assembler_00000000.c
 * 설  명 : 이 프로그램은 SIC/XE 머신을 위한 간단한 Assembler 프로그램의 메인루틴으로,
 * 입력된 파일의 코드 중, 명령어에 해당하는 OPCODE를 찾아 출력한다.
 * 파일 내에서 사용되는 문자열 "00000000"에는 자신의 학번을 기입한다.
 */

 /*
  *
  * 프로그램의 헤더를 정의한다.
  *
  */

#include <stdio.h>
#include <stdlib.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <direct.h>
#include <ctype.h>
#pragma warning(disable:4996)
#define LINE_LENGTH 1024
#define MAX_BLOCKS	10
#define MAX_OPJECTS	100
//#define DEBUG

  // 파일명의 "00000000"은 자신의 학번으로 변경할 것.
#include "my_assembler_20232872.h"

/* ----------------------------------------------------------------------------------
 * 설명 : 사용자로 부터 어셈블리 파일을 받아서 명령어의 OPCODE를 찾아 출력한다.
 * 매계 : 실행 파일, 어셈블리 파일
 * 반환 : 성공 = 0, 실패 = < 0
 * 주의 : 현재 어셈블리 프로그램의 리스트 파일을 생성하는 루틴은 만들지 않았다.
 *		   또한 중간파일을 생성하지 않는다.
 * ----------------------------------------------------------------------------------
 */

//----- 리터럴 테이블에 넣기 전에 저장할 임시 리터럴 테이블 자료구조
literal tmpliteral_table[MAX_LINES];	//LTORG나 파일 끝에 도달하기 전에 저장할 임시 리터럴 배열
size_t tmplitlen_table[MAX_LINES];		//임시 리터럴의 바이트 수의 배열
int tmplit_num = 0;						//임시 리터럴 인덱스
int tmplit_comp = -1;					//LTORG나 파일 끝을 만나서 리터럴 테이블에 쓰여진 원소의 인덱스
//-----

int literal_num = 0;	//리러털 테이블 인덱스

const char* block_name = NULL;	//현재 프로그램 이름

char* opstrs[MAX_LINES];		//토큰의 연산자 문자열, token 구조체와 동기화
int formats[MAX_LINES];			//토큰의 포맷, token 구조체와 동기화
int locctrs[MAX_LINES];			//토큰의 locctr, token 구조체와 동기화

//----- 프로그램별 심볼 테이블을 저장할 자료구조
typedef struct _sector_symbol {	
	char* name;						//프로그램 이름
	symbol sym_table[MAX_LINES];	//프로그램의 심볼 테이블
	size_t length;					//심볼 테이블의 길이
} sector_symbol;

sector_symbol symbols[MAX_BLOCKS];	//프로그램들의 심볼 테이블
int symbol_num = 0;					//심볼 테이블의 프로그램 인덱스
//-----

//----- 프로그램별 오브젝트 코드를 저장할 자료구조
object_code obj_infos[MAX_BLOCKS];	//프로그램들의 오브젝트 코드
int obj_num = -1;					//오브젝트 코드 배열의 프로그램 인덱스
//-----



//프로그램 이름으로 프로그램의 오브젝트 코드 구조체를 찾고 반환
object_code* get_objinfo(const char* block) {
	if (block == NULL) return NULL;
	for (int i = 0; i < obj_num + 1; ++i) {
		if (strcmp(obj_infos[i].name, block) == 0) {
			return &obj_infos[i];
		}
	}
	return NULL;
}



//START, CSECT 만났을때 프로그램 Section 레코드를 추가
void add_sector(const char* sector_name) {
	if (sector_name == NULL) return;
	symbols[symbol_num].name = strdup(sector_name);
	symbols[symbol_num].length = 0;
	++symbol_num;
}



//특정 프로그램 Section의 심볼 테이블에 심볼 추가하기
void add_symbol(const char* sector_name, const char* label) {
	if (sector_name == NULL || label == NULL) return;
	for (int i = 0; i < symbol_num; ++i) {
		if (strcmp(symbols[i].name, sector_name) != 0)
			continue;
		symbol* sym_entry = &(symbols[i].sym_table[symbols[i].length]);
		strcpy(sym_entry->symbol, label);
		sym_entry->addr = locctr;
		++symbols[i].length;
		return;
	}
	add_sector(sector_name);
	symbol* sym_entry = &(symbols[symbol_num - 1].sym_table[symbols[symbol_num - 1].length]);	//추가할 symbol 구조체 포인터
	strcpy(sym_entry->symbol, label);
	sym_entry->addr = locctr;
	++symbols[symbol_num - 1].length;
}



//프로그램 Section 이름으로 Section의 심볼 테이블을 찾고 반환
sector_symbol* get_sectorsymbol(const char* block) {
	if (block == NULL) return NULL;
	for (int i = 0; i < symbol_num; ++i) {
		if (strcmp(symbols[i].name, block) == 0) {
			return &symbols[i];
		}
	}
	return NULL;
}

int main(int args, char* arg[])
{
	if (init_my_assembler() < 0)
	{
		printf("init_my_assembler: 프로그램 초기화에 실패 했습니다.\n");
		return -1;
	}

	if (assem_pass1() < 0)
	{
		printf("assem_pass1: 패스1 과정에서 실패하였습니다.  \n");
		return -1;
	}

	//make_symtab_output("output_symtab.txt");
	//make_literaltab_output("output_littab.txt");
	make_symtab_output(NULL);		//표준출력
	make_literaltab_output(NULL);	//표준출력
	if (assem_pass2() < 0)
	{
		printf(" assem_pass2: 패스2 과정에서 실패하였습니다.  \n");
		return -1;
	}

	make_objectcode_output("output_objectcode.txt");	//파일출력
	//make_objectcode_output(NULL);

	return 0;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 프로그램 초기화를 위한 자료구조 생성 및 파일을 읽는 함수이다.
 * 매계 : 없음
 * 반환 : 정상종료 = 0 , 에러 발생 = -1
 * 주의 : 각각의 명령어 테이블을 내부에 선언하지 않고 관리를 용이하게 하기
 *		   위해서 파일 단위로 관리하여 프로그램 초기화를 통해 정보를 읽어 올 수 있도록
 *		   구현하였다.
 * ----------------------------------------------------------------------------------
 */




//문자열의 뒤에서부터 서브스트링을 찾기, strstr 함수 응용
char* strrstr(const char* str, const char* substr) {
	char* result = NULL;
	char* current = (char*)str;

	if (*substr == '\0') return (char*)(str + strlen(str));

	while ((current = strstr(current, substr)) != NULL) {
		result = current;
		current++;
	}

	return result;
}



//char* 문자열을 int 정수로 변환, 실패하면 -1 반환
int string_to_int(const char* str) {
	if (str == NULL) return NULL;
	char* endptr = NULL;
	int num = (int)strtol(str, &endptr, 10);
	if (*endptr != 0) {
		return -1;
	}
	return num;
}



int init_my_assembler(void)
{
	int result;

	if ((result = init_inst_file("inst_table.txt")) < 0)
		return -1;
	if ((result = init_input_file("input-1.txt")) < 0)
		return -1;
	return result;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 머신을 위한 기계 코드목록 파일(inst_table.txt)을 읽어
 *       기계어 목록 테이블(inst_table)을 생성하는 함수이다.
 *
 *
 * 매계 : 기계어 목록 파일
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 기계어 목록파일 형식은 자유롭게 구현한다. 예시는 다음과 같다.
 *
 *	===============================================================================
 *		   | 이름 | 형식 | 기계어 코드 | 오퍼랜드의 갯수 | \n |
 *	===============================================================================
 *
 * ----------------------------------------------------------------------------------
 */
int init_inst_file(char* inst_file)
{
	FILE* file;
	int errno;

	/* add your code here */
	file = fopen(inst_file, "r");
	if (file == NULL)
		return -1;

	inst_index = 0;
	char buffer[LINE_LENGTH] = { 0 };
	while (fgets(buffer, sizeof(buffer), file)) {	//inst_table.txt를 한 줄씩 읽어서
		inst* _inst = (inst*)malloc(sizeof(inst));
		unsigned int temp_op;
		sscanf(buffer, "%s %x %d %d", &(_inst->str), &(temp_op), &(_inst->format), &(_inst->ops));
		//(이름\t기계어 코드\t형식\t오퍼랜드의 갯수) 형태로 저장된 명령어 정보를 inst 구조체 변수에 대입한다.
		_inst->op = (unsigned char)temp_op;
		inst_table[inst_index++] = _inst;
	}

	fclose(file);
	return 0;

	return errno;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 어셈블리 할 소스코드를 읽어 소스코드 테이블(input_data)를 생성하는 함수이다.
 * 매계 : 어셈블리할 소스파일명
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : 라인단위로 저장한다.
 *
 * ----------------------------------------------------------------------------------
 */
int init_input_file(char* input_file)
{
	FILE* file;
	int errno;

	/* add your code here */
	char path[1024];  // 경로를 저장할 버퍼

	file = fopen(input_file, "r");
	if (file == NULL)
		return -1;

	line_num = 0;
	char buffer[LINE_LENGTH] = { 0 };
	while (fgets(buffer, sizeof(buffer), file)) {	//input.txt를 한 줄씩 읽어서
		buffer[strlen(buffer) - 1] = 0;
		input_data[line_num++] = strdup(buffer);
		//문자열 배열인 input_data의 원소로 저장한다
	}

	fclose(file);
	return 0;

	return errno;
}



//디버그용, 파싱한 토큰의 정보를 출력
void print_token() {
	token* tok = token_table[token_line];
	if (tok == NULL) return;

	printf("locctr : %X\n", locctr);
	if(tok->label)
		printf("label : %s\n", tok->label);
	if (tok->opt)
		printf("operator : %s\n", tok->opt);
	if (tok->comment)
		printf("comment : %s\n", tok->comment);

	printf("operands : ");
	for (int i = 0; i < MAX_OPERAND; ++i) {
		if(tok->operand[i])
			printf("%s ", tok->operand[i]);
	}
	printf("\n");
	printf("nixbpe : ");
	printf("%d\n", tok->nixbpe);
	for (int i = 5; i >= 0; i--) {
		printf("%d", (tok->nixbpe >> i) & 1);
		if (i == 4)
			printf(" ");
	}
	printf("\n");
	printf("\n\n");
}



//delimiter를 기준으로 문자열 쪼개고 쪼개진 서브 스트링의 배열을 반환
char** split_str(const char* str, size_t* length, const char* delim) {
	if (str == NULL) return NULL;
	char* tmpstr = strdup(str);	//strtok로 문자열을 쪼개면 기존 문자열이 훼손될 수 있으므로 임시 문자열로 strtok 실행
	size_t len = 0;
	char** splits = (char**)malloc(256 * sizeof(char*));

	char* split = strtok(tmpstr, delim);	//delim으로 어셈블리어 각 줄을 토큰화
	while (split != NULL && len < 256) {	//최대 1024개의 토큰받기
		splits[len++] = strdup(split);
		split = strtok(NULL, delim);
	}
	free(tmpstr);
	*length = len;
	return splits;
}



//문자열 배열을 전체 메모리 해제
void free_strarray(char** strarr, size_t len) {
	if (strarr == NULL) return;
	for (int i = 0; i < len; ++i) {
		if (strarr[i] == NULL)
			continue;
		free(strarr[i]);
	}
	free(strarr);
}



//C'EOF', X'05', =C'EOF' 같은 상수들을 파싱
char* parse_literal(const char* lit, size_t* len, int* equalflag) {
	if (lit == NULL) return NULL;
	char* current = lit;
	*equalflag = 0;
	if (*current == '=') {	//=으로 시작하는 리터럴도 처리
		*equalflag = 1;
		++current;
	}
	char* left = strstr(lit, "'");
	char* right = strrstr(lit, "'");
	if (left == NULL) return NULL;
	size_t litlen = right - left - 1;
	char* literal = (char*)malloc(litlen + 1);
	strncpy(literal, left + 1, litlen);	//따옴표 사이의 값을 추출
	literal[litlen] = 0;
	*len = (*current == 'X') ? litlen / 2 : litlen;
	return literal;
}



//명령어의 피연산자가 리터럴인지 확인하고 리터럴이면 임시 테이블에 저장
void check_litoperand(const char* operand) {
	if (operand == NULL) return;
	size_t litlen = 0;
	int equalflag = 0;
	char* literal = parse_literal(operand, &litlen, &equalflag);
	if (literal && equalflag) {		//=으로 시작하는 리터럴이면 임시 리터럴 테이블에 추가
		tmpliteral_table[tmplit_num].literal = strdup(literal);
		tmpliteral_table[tmplit_num].addr = locctr;
		tmplitlen_table[tmplit_num] = litlen;
		++tmplit_num;
	}
}



//함수 전방 선언
symbol search_symbol(const char* block, const char* sym);



//레이블의 주소를 찾고 전체 피연산자 값에 +-연산을 수행, 없는 레이블이면 modification record로 추가
void accumlate_label(const char* block, const char* label, int* addr, int neg, int offset, int modsize) {
	if (block == NULL || label == NULL || addr == NULL) return;
	symbol sym = search_symbol(block, label);
	if (sym.addr >= 0) {
		*addr += neg ? -(sym.addr) : sym.addr;	//neg 부호에 따라 주소값에다가 더하기, 빼기 연산 수행
		return;
	}
	if (offset < 0)
		return;
	object_code* objcode = get_objinfo(block_name);
	objcode->mods[objcode->modcnt++] = (mod_record){ locctr + offset, modsize, neg, strdup(label), objcode->modcnt };
	//심볼 테이블에서 못찾으면 오프셋, 수정길이 등을 매개변수로 받아서 modification record로 추가
}



//레이블로 이루어진 피연산자의 전체 값을 계산
int get_labelvalue(const char* block, const char* operand, int offset, int modsize) {
	if (block == NULL || operand == NULL) return -1;
	if (strcmp(operand, "*") == 0) return locctr;
	char* label = strdup(operand);
	int addr = 0;

	int negflag = 0;
	char* start = label;
	for (int i = 0; i < strlen(label); ++i) {
		if (label[i] == '+' || label[i] == '-') {
			char opt = label[i];
			label[i] = 0;	//+, - 자리에 널 대입해서 문자열 분리
			accumlate_label(block, start, &addr, negflag, offset, modsize);
			negflag = (opt == '-') ? 1 : 0;	//연산자가 -면 neg 에 1대입
			start = label + i + 1;			//다음 심볼 문자열 시작점
		}
	}
	if (start != NULL) {
		accumlate_label(block, start, &addr, negflag, offset, modsize);
	}
	return addr;
}



//명령어와 토큰을 분석해서 증가시킬 locctr 값을 계산, 몇몇 연산자의 경우에는 추가작업 실행
int get_locctr_add(inst instruction, token assemtok) {
	if ((assemtok.nixbpe >> 0) & 1) {
		return 4;
	}
	if (assemtok.operand[0] == NULL) {
		return instruction.format;
	}

	if (strcmp(instruction.str, "RESB") == 0) {	//RESB면 피연산자를 정수 파싱해서 정수 * 1 반환
		obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ 0, 0, locctr };
		int opdnum = string_to_int(assemtok.operand[0]);
		return opdnum * 1;
	}
	else if (strcmp(instruction.str, "RESW") == 0) {	//RESW면 피연산자를 정수 파싱해서 정수 * 3 반환
		obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ 0, 0, locctr };
		int opdnum = string_to_int(assemtok.operand[0]);
		return opdnum * 3;
	}
	else if (strcmp(instruction.str, "BYTE") == 0) {	//BYTE면 피연산자를 문자열 파싱해서 16진수의 길이 반환
		size_t litlen = 0;
		int equalflag = 0;
		char* literal = parse_literal(assemtok.operand[0], &litlen, &equalflag);
		unsigned int litcode = convert_literal(literal, &litlen);
		obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ litcode, litlen, locctr };

		return litlen;
	}
	else if (strcmp(instruction.str, "WORD") == 0) {	//WORD면 피연산자를 정수 파싱 시도하고 안되면 심볼 파싱 실행
		size_t litlen = 0;
		int opdnum = string_to_int(assemtok.operand[0]);
		if (opdnum >= 0) {
			obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ opdnum, 3, locctr };
		}
		else {
			int value = get_labelvalue(block_name, assemtok.operand[0], 0, 6);
			obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ value, 3, locctr };
		}
		return 3;
	}
	else if (strcmp(instruction.str, "EQU") == 0) {	//EQU면 피연산자를 정수 파싱 시도하고 안되면 심볼 파싱 실행
		size_t litlen = 0;
		int opdnum = string_to_int(assemtok.operand[0]);
		opdnum = (opdnum >= 0) ? opdnum : get_labelvalue(block_name, assemtok.operand[0], -1, 0);
		sector_symbol* blocksym = get_sectorsymbol(block_name);
		blocksym->sym_table[blocksym->length - 1].addr = opdnum;
		return 0;
	}
	return instruction.format;
}



//object code 구조체 초기화
void init_objcode() {
	obj_infos[obj_num].name = NULL;
	obj_infos[obj_num].start = 0;
	obj_infos[obj_num].length = 0;
	obj_infos[obj_num].defs = (char**)malloc(MAX_OPERAND * sizeof(char*));
	obj_infos[obj_num].deflen = 0;
	obj_infos[obj_num].refs = (char**)malloc(MAX_OPERAND * sizeof(char*));
	obj_infos[obj_num].reflen = 0;
	obj_infos[obj_num].bincnt = 0;
	obj_infos[obj_num].modcnt = 0;
}



//EOF나 05 등의 리터럴을 16진수 코드로 변환
unsigned int convert_literal(const char* literal, size_t* len) {
	if (literal == NULL || len == NULL) return NULL;

	char* endptr = NULL;
	unsigned int bincode = (unsigned int)strtoul(literal, &endptr, 16);	//문자열 그대로 16진수 변환 시도(ex. "05" -> 0x05)
	if (*endptr == 0) {
		*len = strlen(literal) / 2;
		return bincode;
	}

	*len = 0;
	int shift = 0;
	bincode = 0;
	for (int i = strlen(literal) - 1; i >= 0; --i) {	//16진수 변환 실패하면 각 문자의 16진수 아스키코드 추가
		if (shift >= 4) break;
		bincode |= (literal[i] << (shift * 8));
		++(*len);
		++shift;
	}
	return bincode;
}



//토큰과 연산자를 분석해서 필요하면 새로운 프로그램의 object code를 추가하고 레이블이 존재한다면 추가
void add_sector_symbol(const char* op) {
	if (op == NULL) return;
	if (strcmp(op, "START") == 0 || strcmp(op, "CSECT") == 0) {	//새로운 Section 진입
		if (block_name != NULL) {
			obj_infos[obj_num].length = locctr;
		}
		block_name = token_table[token_line]->label;

		int start = 0;
		if (strcmp(op, "START") == 0) {
			start = string_to_int(token_table[token_line]->operand[0]);	//START면 시작위치 파싱
		}
		++obj_num;
		init_objcode();
		obj_infos[obj_num].start = start;
		obj_infos[obj_num].name = strdup(block_name);
		locctr = 0;
	}
	add_symbol(block_name, token_table[token_line]->label);	//레이블 존재하면 Section에 심볼 추가
}



//임시 리터럴 테이블에 저장된 리터럴을 리터럴 테이블에 저장
void ltorg(const char* op, int checkop) {
	if (checkop && op == NULL) return;
	if (checkop && strcmp(op, "LTORG") != 0) return;
	while (tmplit_comp < tmplit_num - 1) {	//임시 리터럴 테이블의 마지막 원소까지 접근
		literal entry = tmpliteral_table[++tmplit_comp];
		int haslit = 0;
		for (int i = 0; i < literal_num; ++i) {
			if (strcmp(entry.literal, literal_table[i].literal) == 0) {	//리터럴 테이블에 추가할 리터럴에 중복되는지 검사
				haslit = 1;
				break;
			}
		}
		if (haslit) continue;	//중복되지 않은 리터럴이며 리터럴 테이블에 추가
		literal_table[literal_num].literal = strdup(entry.literal);
		literal_table[literal_num].addr = locctr;
		size_t litlen = 0;
		unsigned int litcode = convert_literal(entry.literal, &litlen);	//리터럴 16진수 코드 변환하고 Section의 object code에 추가
		obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ litcode, litlen, locctr };
		locctr += tmplitlen_table[tmplit_comp];
		++literal_num;
	}
}



//피연산자를 분석해서 immediate, indirect 등의 주소 계산 방식을 지정
void check_ni(const char* operand) {
	if (operand == NULL) return NULL;
	if (operand[0] == '#') {	//immediate
		token_table[token_line]->nixbpe &= ~(1 << 5);
	}
	else if (operand[0] == '@') {	//indirect
		token_table[token_line]->nixbpe &= ~(1 << 4);
	}
}



//레이블 파싱하고 토큰에 저장, 성공 -> 1, 실패 -> 0
int parse_label(const char** lexes, size_t* idx, size_t lexlen) {
	if (lexes == NULL) return 0;
	if (*idx >= lexlen) return 0;
	token_table[token_line]->label = strdup(lexes[*idx]);
	++(*idx);
	return 1;
}



//연산자 파싱하고 토큰에 저장, 성공 -> 1, 실패 -> 0
int parse_operator(int opidx, size_t* idx, size_t lexlen) {
	if (opidx < 0) return 0;
	char opcodestr[3] = { 0 };
	snprintf(opcodestr, 3, "%02X", inst_table[opidx]->op);	//두 자리 16진수로 문자열로 저장
	token_table[token_line]->opt = strdup(opcodestr);
	++(*idx);
	return 1;
}



//피연산자 파싱하고 토큰에 저장, 성공 -> 1, 실패 -> 0
int parse_operands(const char** lexes, size_t* idx, size_t lexlen, int opds) {
	if (lexes == NULL) return 0;
	if (opds <= 0) return 1;
	if (*idx >= lexlen && opds > 0) return 0;
	if (strcmp(lexes[*idx], "EXTDEF") == 0 || strcmp(lexes[*idx], "EXTREF") == 0) {
		++(*idx);
		return 1;
	}

	check_litoperand(lexes[*idx]);	//리터럴 피연산자인지 검사
	int flag = 1;
	char* tmpstr = strdup(lexes[*idx]);

	char* operand = strtok(tmpstr, ",");
	check_ni(operand);	//immdiate, indirect 검사
	for (int i = 0; i < opds; ++i) {	//명령어의 ops 만큼 피연산자 파싱
		if (operand == NULL) {
			flag = 0;
			break;
		}
		token_table[token_line]->operand[i] = strdup(operand);
		operand = strtok(NULL, ",");	//쉼표를 기준으로 피연산자 분리
	}
	
	if (operand && strcmp(operand, "X") == 0) {	//나머지 피연산자에 X가 있으면 index addressing
		token_table[token_line]->nixbpe |= (1 << 3);
		flag = 1;
	}

	free(tmpstr);
	++(*idx);
	return flag;
}



//주석 파싱하고 토큰에 저장, 성공 -> 1, 실패 -> 0
int parse_comment(const char** lexes, size_t* idx, size_t lexlen) {
	if (lexes == NULL) return 0;
	if (*idx >= lexlen) return 1;

	char comment[100] = { 0 };
	strcpy(comment, lexes[*idx]);
	++(*idx);
	while (*idx < lexlen) {	//명령어 끝까지 주석으로 추가
		strcat(comment, " ");
		strcat(comment, lexes[*idx]);
		++(*idx);
	}

	strcpy(token_table[token_line]->comment, comment);
	++(*idx);
	return 1;
}



//리터럴 테이블을 탐색해서 해당 리터럴 구조체 반환
literal search_literal(const char* lit) {
	if (lit == NULL) return (literal) { "", -1 };
	for (int i = 0; i < literal_num; ++i) {
		if (strcmp(lit, literal_table[i].literal) == 0) {
			return literal_table[i];
		}
	}
	return (literal) { "", -1 };
}



//심볼 테이블을 탐색해서 해당 심볼 구조체 반환
symbol search_symbol(const char* block, const char* sym) {
	if (block == NULL || sym == NULL) return (symbol) { "", -1 };
	sector_symbol* blocksym = get_sectorsymbol(block);
	for (int i = 0; i < blocksym->length; ++i) {
		if (strcmp(blocksym->sym_table[i].symbol, sym) == 0) {
			return blocksym->sym_table[i];
		}
	}
	return (symbol) { "", -1 };
}



//연산자와 피연산자를 분석해서 EXTDEF EXTREF 레이블 저장
void check_ext(const char* op, const char* opd) {
	if (op == NULL || opd == NULL) return;
	if (strcmp(op, "EXTDEF") && strcmp(op, "EXTREF")) return;
	size_t len = 0;
	char** opds = split_str(opd, &len, ",");
	if (strcmp(op, "EXTDEF") == 0) {	//EXTDEF 대상 심볼 추가
		for (int i = 0; i < len; ++i) {
			const char* def = opds[i];
			obj_infos[obj_num].defs[obj_infos[obj_num].deflen++] = strdup(def);
		}
	}
	else if (strcmp(op, "EXTREF") == 0) {	//EXTREF 대상 심볼 추가
		for (int i = 0; i < len; ++i) {
			const char* ref = opds[i];
			obj_infos[obj_num].refs[obj_infos[obj_num].reflen++] = strdup(ref);
		}
	}
	free_strarray(opds, len);
}



//token 구조체 초기화
void init_token() {
	token_table[token_line] = (token*)malloc(sizeof(token));
	token_table[token_line]->label = NULL;
	token_table[token_line]->opt = NULL;
	for (int i = 0; i < MAX_OPERAND; ++i) {
		token_table[token_line]->operand[i] = NULL;
	}
	strcpy(token_table[token_line]->comment, "");
	token_table[token_line]->nixbpe = 0x32; //0011 0010 으로 초기화
}

/* ----------------------------------------------------------------------------------
 * 설명 : 소스 코드를 읽어와 토큰단위로 분석하고 토큰 테이블을 작성하는 함수이다.
 *        패스 1로 부터 호출된다.
 * 매계 : 파싱을 원하는 문자열
 * 반환 : 정상종료 = 0 , 에러 < 0
 * 주의 : my_assembler 프로그램에서는 라인단위로 토큰 및 오브젝트 관리를 하고 있다.
 * ----------------------------------------------------------------------------------
 */
int token_parsing(char* str)
{
	/* add your code here */
	if (str == NULL) return -1;
	size_t lexlen = 0;
	char** lexemes = split_str(str, &lexlen, " \t");

#ifdef DEBUG
	for (int i = 0; i < lexlen; ++i) {
		printf("%d : %s\n", i, lexemes[i]);
	}
	printf("\n");
#endif // DEBUG

	if (lexemes[0][0] == '.') {
		free_strarray(lexemes, lexlen);
		return 0;
	}

	int labeladdr = 0;
	int flag = 1;
	size_t lexidx = 0;
	locctrs[token_line] = locctr;
	init_token();
	int opidx = search_opcode(lexemes[lexidx]);
	if (opidx < 0) {	//명령어의 첫 단어가 연산자가 아니면 레이블부터 파싱
		flag = flag && parse_label(lexemes, &lexidx, lexlen);
	}
	//이후부터 차례대로 연산자, 피연산자, 주석 파싱 실행
	opidx = search_opcode(lexemes[lexidx]);
	const char* opstr = lexemes[lexidx];
	flag = flag && parse_operator(opidx, &lexidx, lexlen);
	const char* opd = lexemes[lexidx];
	flag = flag && parse_operands(lexemes, &lexidx, lexlen, inst_table[opidx]->ops);
	flag = flag && parse_comment(lexemes, &lexidx, lexlen);
	ltorg(opstr, 1);	//LTORG 명령어인지 확인
	add_sector_symbol(opstr);	//새로운 Section이나 새로운 심볼을 추가해야 하는지 확인
	check_ext(opstr, opd);	//EXTREF, EXTDEF 확인
	opstrs[token_line] = strdup(opstr);
	char nixbpe = token_table[token_line]->nixbpe;
	formats[token_line] = (nixbpe >> 0) & 1 ? 4 : inst_table[opidx]->format;	//e 비트 검사해서 format 지정

#ifdef DEBUG
	print_token();
#endif // DEBUG

	locctr += get_locctr_add(*inst_table[opidx], *token_table[token_line]);	//다음 명령어 분석을 위한 locctr 증가
	++token_line;

	free_strarray(lexemes, lexlen);
	return flag - 1;
}



/* ----------------------------------------------------------------------------------
 * 설명 : 입력 문자열이 기계어 코드인지를 검사하는 함수이다.
 * 매계 : 토큰 단위로 구분된 문자열
 * 반환 : 정상종료 = 기계어 테이블 인덱스, 에러 < 0
 * 주의 : 기계어 목록 테이블에서 특정 기계어를 검색하여, 해당 기계어가 위치한 인덱스를 반환한다.
 *        '+JSUB'과 같은 문자열에 대한 처리는 자유롭게 처리한다.
 *
 * ----------------------------------------------------------------------------------
 */
int search_opcode(char* str)
{
	/* add your code here */
	if (str == NULL) return -1;
	const char* targetop = str;
	if (str[0] == '+') {	//4형식 명령어일 경우 '+'뒷 문자열이 탐색 대상, e bit는 1로 수정, pc bit는 0으로 수정
		token_table[token_line]->nixbpe |= (1 << 0);
		token_table[token_line]->nixbpe &= ~(1 << 1);
		++targetop;
	}

	for (int i = 0; i < inst_index; ++i) {
		if (strcmp(targetop, inst_table[i]->str) != 0)	//명령어 구조체 배열을 순회하면서 토큰이랑 같은지 확인
			continue;

		if (str[0] == '+' && inst_table[i]->format != 3) {	//4형식 명령어인데 형식이 3인지 확인, 아니면 에러
			return -1;
		}
		return i;
	}
	return -1;	//끝까지 명령어 못찾으면 에러
}

/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 위한 패스1과정을 수행하는 함수이다.
*		   패스1에서는..
*		   1. 프로그램 소스를 스캔하여 해당하는 토큰단위로 분리하여 프로그램 라인별 토큰
*		   테이블을 생성한다.
*          2. 토큰 테이블은 token_parsing()을 호출하여 설정한다.
*          3. assem_pass2 과정에서 사용하기 위한 심볼테이블 및 리터럴 테이블을 생성한다.
*
* 매계 : 없음
* 반환 : 정상 종료 = 0 , 에러 = < 0
* 주의 : 현재 초기 버전에서는 에러에 대한 검사를 하지 않고 넘어간 상태이다.
*	  따라서 에러에 대한 검사 루틴을 추가해야 한다.
*
* -----------------------------------------------------------------------------------
*/
static int assem_pass1(void)
{
	/* add your code here */

	/* input_data의 문자열을 한줄씩 입력 받아서
	 * token_parsing()을 호출하여 _token에 저장
	 */
	token_line = 0;
	locctr = 0;
	for (int i = 0; i < line_num; ++i) {
		if (token_parsing(input_data[i]) < 0) {
			return -1;
		}
	}
	ltorg(NULL, 0);
	if (block_name != NULL) {
		obj_infos[obj_num++].length = locctr;
	}

	make_opcode_output("output.txt");	//opcode가 같이 적혀져 있는 어셈블리어 파일
	return 0; 
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 소스코드 명령어 앞에 OPCODE가 기록된 코드를 파일에 출력한다.
*        파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*        프로젝트 1에서는 불필요하다.
 *
* -----------------------------------------------------------------------------------
*/
void make_opcode_output(char* file_name)
{
	/* add your code here */
	//FILE* fp = fopen(file_name, "w");
	//if (fp == NULL)
	//	return;

	//for (int i = 0; i < line_num; ++i) {
	//	fprintf(fp, "%s\n", input_data[i]);	//opcode가 적혀진 어셈블리어 줄 단위로 output.txt에 입력
	//}
	//fclose(fp);
	return;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 SYMBOL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명 혹은 경로
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_symtab_output(char* file_name)
{
	/* add your code here */
	FILE* fp = file_name ? fopen(file_name, "w") : stdout;
	if (fp == NULL) return;

	for (int i = 0; i < symbol_num; ++i) {
		for (int j = 0; j < symbols[i].length; ++j) {
			symbol entry = symbols[i].sym_table[j];
			fprintf(fp, "%s\t\t\t%X\n", entry.symbol, entry.addr);
		}
		fprintf(fp, "\n");
	}
	fprintf(fp, "\n");
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 LITERAL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*
* -----------------------------------------------------------------------------------
*/
void make_literaltab_output(char* file_name)
{
	/* add your code here */
	FILE* fp = file_name ? fopen(file_name, "w") : stdout;
	if (fp == NULL) return;

	for (int i = 0; i < literal_num; ++i) {
		fprintf(stdout, "%s\t\t\t%X\n", literal_table[i].literal, literal_table[i].addr);
	}
	fprintf(fp, "\n");
}



//레지스터별 번호 얻기
unsigned char get_registernum(const char* rg) {
	if (rg == NULL) return -1;
	if (strcmp(rg, "A") == 0) {
		return 0;
	}
	if (strcmp(rg, "X") == 0) {
		return 1;
	}
	if (strcmp(rg, "L") == 0) {
		return 2;
	}
	if (strcmp(rg, "B") == 0) {
		return 3;
	}
	if (strcmp(rg, "S") == 0) {
		return 4;
	}
	if (strcmp(rg, "T") == 0) {
		return 5;
	}
	if (strcmp(rg, "F") == 0) {
		return 6;
	}
	if (strcmp(rg, "PC") == 0) {
		return 8;
	}
	if (strcmp(rg, "SW") == 0) {
		return 9;
	}
	return -1;
}



//연산자와 nixbpe까지만 반영된 16진수 코드에 피연산자 값도 채우기
unsigned int fill_operand(unsigned int binary, int idx) {
	if (formats[idx] == 2) {	//2형식 명령어면 피연산자를 레지스터 번호로 변환
		int insert_idx = 1;
		for (int i = 0; i < MAX_OPERAND; ++i) {
			if (insert_idx < 0)
				return binary;
			if (token_table[idx]->operand[i] == NULL)
				continue;
			unsigned char rgnum = get_registernum((token_table[idx]->operand[i]));
			binary |= (rgnum << (insert_idx * 4));
			--insert_idx;
		}
	}
	else if (formats[idx] == 3 || formats[idx] == 4) {	//3, 4형식이면 심볼 탐색, 정수 변환, 리터럴 탐색을 실행
		if (token_table[idx]->operand[0] == NULL) {
			token_table[idx]->nixbpe &= ~(1 << 1);
			return binary;
		}
		char* start = token_table[idx]->operand[0];
		int num = -1;
		if (*start == '@' || *start == '#') {
			++start;
		}
		symbol sym = search_symbol(block_name, start);	//단일 심볼 탐색 시도
		if (sym.addr >= 0) {
			num = sym.addr - (locctrs[idx] + formats[idx]);
			binary |= ((num & 0xFFF) << 0);
		}
		else if ((num = string_to_int(start)) >= 0) {	//정수 변환 시도
			token_table[idx]->nixbpe &= ~(1 << 1);
			binary |= (num << 0);
		}
		else {
			size_t litlen = 0;
			int equalflag = 0;
			char* litstr = parse_literal(start, &litlen, &equalflag);
			literal lit = search_literal(litstr);	//리터럴 탐색 시도
			free(litstr);
			if (lit.addr >= 0) {
				num = lit.addr - (locctrs[idx] + formats[idx]);
				binary |= ((num & 0xFFF) << 0);
			}
			else {	//+, -로 이루어진 심볼값 계산 시도, 없는 심볼이면 0으로 채우고 modification record 추가
				num = get_labelvalue(block_name, start, 1, formats[idx] * 2 - 3);
				binary += num;
			}
		}
	}
	return binary;
}



/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 기계어 코드로 바꾸기 위한 패스2 과정을 수행하는 함수이다.
*		   패스 2에서는 프로그램을 기계어로 바꾸는 작업은 라인 단위로 수행된다.
*		   다음과 같은 작업이 수행되어 진다.
*		   1. 실제로 해당 어셈블리 명령어를 기계어로 바꾸는 작업을 수행한다.
* 매계 : 없음
* 반환 : 정상종료 = 0, 에러발생 = < 0
* 주의 :
* -----------------------------------------------------------------------------------
*/
static int assem_pass2(void)
{
	/* add your code here */
	object_code* objinfo = NULL;
	for (int i = 0; i < token_line; ++i) {
		locctr = locctrs[i];
		token* tokent = token_table[i];
		if (strcmp(opstrs[i], "START") == 0 || strcmp(opstrs[i], "CSECT") == 0) {	//현재 pass 중인 Section 이름 설정
			block_name = tokent->label;
			objinfo = get_objinfo(block_name);	//현재 pass 중인 Section object code 얻기
			continue;
		}

		if (strcmp(tokent->opt, "FF") == 0) continue;

		unsigned int code = (unsigned int)strtoul(tokent->opt, NULL, 16);	//명령어 코드 변환
		code = code << (formats[i] - 1) * 8;	//format 만큼 비트 확장하기
		code = fill_operand(code, i);	//피연산자 비트 채우기
		if (formats[i] >= 3) {	//3형식 이상이면 nixbpe 채우기
			unsigned char ni = (tokent->nixbpe >> 4) & 0xF;
			code |= (ni << (formats[i] - 1) * 8);
			unsigned char xbpe = (tokent->nixbpe >> 0) & 0xF;
			code |= (xbpe << (formats[i] - 1) * 8 - 4);
		}
		objinfo->bins[objinfo->bincnt++] = (binary){ code, formats[i], locctrs[i] };	//최종 16진수 코드 저장
	}
}



//locctr를 기준으로 프로그램의 16진수 코드 정렬하는 비교 함수
int compare_locctr(const void* a, const void* b) {
	binary* bin1 = (binary*)a;
	binary* bin2 = (binary*)b;
	if(bin1->start != bin2->start)
		return bin1->start - bin2->start;
	return bin1->len - bin2->len;
}


//modification record의 시작위치를 기준으로 프로그램의 mod record를 정렬하는 비교 함수
int compare_start(const void* a, const void* b) {
	mod_record* mod1 = (mod_record*)a;
	mod_record* mod2 = (mod_record*)b;
	if (mod1->pos == mod2->pos)
		return mod1->idx - mod2->idx;
	return mod1->pos - mod2->pos;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 object code이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 파일이 NULL값이 들어온다면 프로그램의 결과를 stdout으로 보내어
*        화면에 출력해준다.
*        명세서의 주어진 출력 결과와 완전히 동일해야 한다.
*        예외적으로 각 라인 뒤쪽의 공백 문자 혹은 개행 문자의 차이는 허용한다.
*
* -----------------------------------------------------------------------------------
*/
void make_objectcode_output(char* file_name)
{
	/* add your code here */
	FILE* fp = file_name ? fopen(file_name, "w") : stdout;
	if (fp == NULL) return;

	char buffer[MAX_OPJECTS] = { 0 };
	for (int i = 0; i < obj_num; ++i) {
		qsort(obj_infos[i].bins, obj_infos[i].bincnt, sizeof(binary), compare_locctr);

		//헤더 정보 출력
		memset(buffer, 0, MAX_OPJECTS);
		snprintf(buffer, MAX_OPJECTS, "H%-6s%06X%06X\n", obj_infos[i].name, obj_infos[i].start, obj_infos[i].length);
		fprintf(fp, buffer);

		//EXTDEF 정보 출력
		memset(buffer, 0, MAX_OPJECTS);
		for (int j = 0; j < obj_infos[i].deflen; ++j) {
			symbol sym = search_symbol(obj_infos[i].name, obj_infos[i].defs[j]);
			if (sym.addr >= 0) {
				snprintf(buffer + strlen(buffer), MAX_OPJECTS, "%-6s%06X", sym.symbol, sym.addr);
			}
		}
		if (strcmp(buffer, "") != 0) {
			fprintf(fp, "D%s\n", buffer);
		}

		//EXTREF 정보 출력
		memset(buffer, 0, MAX_OPJECTS);
		for (int j = 0; j < obj_infos[i].reflen; ++j) {
			snprintf(buffer + strlen(buffer), MAX_OPJECTS, "%-6s", obj_infos[i].refs[j]);
		}
		if (strcmp(buffer, "") != 0) {
			fprintf(fp, "R%s\n", buffer);
		}

		//TEXT 정보 출력
		memset(buffer, 0, MAX_OPJECTS);
		int total_len = 0;
		int start_loc = 0;
		for (int j = 0; j < obj_infos[i].bincnt; ++j) {
			if (total_len + obj_infos[i].bins[j].len > 30 || obj_infos[i].bins[j].len == 0) {
				//길이가 30 바이트 초과거나 순차적인 주소로 연결되지 않으면 줄바꿈
				if (strcmp(buffer, "") == 0) continue;
				fprintf(fp, "T%06X%02X%s\n", start_loc, total_len, buffer);
				memset(buffer, 0, MAX_OPJECTS);
				total_len = 0;
				if (obj_infos[i].bins[j].len == 0) continue;
			}
			if (strcmp(buffer, "") == 0) {
				start_loc = obj_infos[i].bins[j].start;
			}
			snprintf(buffer + strlen(buffer), MAX_OPJECTS, "%0*X", obj_infos[i].bins[j].len * 2, obj_infos[i].bins[j].code);
			//미리 저장한 코드 바이트 길이만큼 출력 형식 지정
			total_len += obj_infos[i].bins[j].len;
		}
		if (strcmp(buffer, "") != 0) {
			fprintf(fp, "T%06X%02X%s\n", start_loc, total_len, buffer);
		}

		//modification record 출력
		memset(buffer, 0, MAX_OPJECTS);
		for (int j = 0; j < obj_infos[i].modcnt; ++j) {
			qsort(obj_infos[i].mods, obj_infos[i].modcnt, sizeof(mod_record), compare_start);
			char opt = obj_infos[i].mods[j].neg ? '-' : '+';
			snprintf(buffer, MAX_OPJECTS, "%06X%02X%c%s", obj_infos[i].mods[j].pos, obj_infos[i].mods[j].bytes, opt, obj_infos[i].mods[j].label);
			fprintf(fp, "M%s\n", buffer);
		}

		//END 정보 출력
		memset(buffer, 0, MAX_OPJECTS);
		if (i == 0) {
			snprintf(buffer, MAX_OPJECTS, "%06X", 0);
		}
		fprintf(fp, "E%s\n\n", buffer);
	}
}