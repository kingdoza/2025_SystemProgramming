/*
 * ���ϸ� : my_assembler_00000000.c
 * ��  �� : �� ���α׷��� SIC/XE �ӽ��� ���� ������ Assembler ���α׷��� ���η�ƾ����,
 * �Էµ� ������ �ڵ� ��, ��ɾ �ش��ϴ� OPCODE�� ã�� ����Ѵ�.
 * ���� ������ ���Ǵ� ���ڿ� "00000000"���� �ڽ��� �й��� �����Ѵ�.
 */

 /*
  *
  * ���α׷��� ����� �����Ѵ�.
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

  // ���ϸ��� "00000000"�� �ڽ��� �й����� ������ ��.
#include "my_assembler_20232872.h"

/* ----------------------------------------------------------------------------------
 * ���� : ����ڷ� ���� ����� ������ �޾Ƽ� ��ɾ��� OPCODE�� ã�� ����Ѵ�.
 * �Ű� : ���� ����, ����� ����
 * ��ȯ : ���� = 0, ���� = < 0
 * ���� : ���� ����� ���α׷��� ����Ʈ ������ �����ϴ� ��ƾ�� ������ �ʾҴ�.
 *		   ���� �߰������� �������� �ʴ´�.
 * ----------------------------------------------------------------------------------
 */

//----- ���ͷ� ���̺� �ֱ� ���� ������ �ӽ� ���ͷ� ���̺� �ڷᱸ��
literal tmpliteral_table[MAX_LINES];	//LTORG�� ���� ���� �����ϱ� ���� ������ �ӽ� ���ͷ� �迭
size_t tmplitlen_table[MAX_LINES];		//�ӽ� ���ͷ��� ����Ʈ ���� �迭
int tmplit_num = 0;						//�ӽ� ���ͷ� �ε���
int tmplit_comp = -1;					//LTORG�� ���� ���� ������ ���ͷ� ���̺� ������ ������ �ε���
//-----

int literal_num = 0;	//������ ���̺� �ε���

const char* block_name = NULL;	//���� ���α׷� �̸�

char* opstrs[MAX_LINES];		//��ū�� ������ ���ڿ�, token ����ü�� ����ȭ
int formats[MAX_LINES];			//��ū�� ����, token ����ü�� ����ȭ
int locctrs[MAX_LINES];			//��ū�� locctr, token ����ü�� ����ȭ

//----- ���α׷��� �ɺ� ���̺��� ������ �ڷᱸ��
typedef struct _sector_symbol {	
	char* name;						//���α׷� �̸�
	symbol sym_table[MAX_LINES];	//���α׷��� �ɺ� ���̺�
	size_t length;					//�ɺ� ���̺��� ����
} sector_symbol;

sector_symbol symbols[MAX_BLOCKS];	//���α׷����� �ɺ� ���̺�
int symbol_num = 0;					//�ɺ� ���̺��� ���α׷� �ε���
//-----

//----- ���α׷��� ������Ʈ �ڵ带 ������ �ڷᱸ��
object_code obj_infos[MAX_BLOCKS];	//���α׷����� ������Ʈ �ڵ�
int obj_num = -1;					//������Ʈ �ڵ� �迭�� ���α׷� �ε���
//-----



//���α׷� �̸����� ���α׷��� ������Ʈ �ڵ� ����ü�� ã�� ��ȯ
object_code* get_objinfo(const char* block) {
	if (block == NULL) return NULL;
	for (int i = 0; i < obj_num + 1; ++i) {
		if (strcmp(obj_infos[i].name, block) == 0) {
			return &obj_infos[i];
		}
	}
	return NULL;
}



//START, CSECT �������� ���α׷� Section ���ڵ带 �߰�
void add_sector(const char* sector_name) {
	if (sector_name == NULL) return;
	symbols[symbol_num].name = strdup(sector_name);
	symbols[symbol_num].length = 0;
	++symbol_num;
}



//Ư�� ���α׷� Section�� �ɺ� ���̺� �ɺ� �߰��ϱ�
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
	symbol* sym_entry = &(symbols[symbol_num - 1].sym_table[symbols[symbol_num - 1].length]);	//�߰��� symbol ����ü ������
	strcpy(sym_entry->symbol, label);
	sym_entry->addr = locctr;
	++symbols[symbol_num - 1].length;
}



//���α׷� Section �̸����� Section�� �ɺ� ���̺��� ã�� ��ȯ
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
		printf("init_my_assembler: ���α׷� �ʱ�ȭ�� ���� �߽��ϴ�.\n");
		return -1;
	}

	if (assem_pass1() < 0)
	{
		printf("assem_pass1: �н�1 �������� �����Ͽ����ϴ�.  \n");
		return -1;
	}

	//make_symtab_output("output_symtab.txt");
	//make_literaltab_output("output_littab.txt");
	make_symtab_output(NULL);		//ǥ�����
	make_literaltab_output(NULL);	//ǥ�����
	if (assem_pass2() < 0)
	{
		printf(" assem_pass2: �н�2 �������� �����Ͽ����ϴ�.  \n");
		return -1;
	}

	make_objectcode_output("output_objectcode.txt");	//�������
	//make_objectcode_output(NULL);

	return 0;
}

/* ----------------------------------------------------------------------------------
 * ���� : ���α׷� �ʱ�ȭ�� ���� �ڷᱸ�� ���� �� ������ �д� �Լ��̴�.
 * �Ű� : ����
 * ��ȯ : �������� = 0 , ���� �߻� = -1
 * ���� : ������ ��ɾ� ���̺��� ���ο� �������� �ʰ� ������ �����ϰ� �ϱ�
 *		   ���ؼ� ���� ������ �����Ͽ� ���α׷� �ʱ�ȭ�� ���� ������ �о� �� �� �ֵ���
 *		   �����Ͽ���.
 * ----------------------------------------------------------------------------------
 */




//���ڿ��� �ڿ������� ���꽺Ʈ���� ã��, strstr �Լ� ����
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



//char* ���ڿ��� int ������ ��ȯ, �����ϸ� -1 ��ȯ
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
 * ���� : �ӽ��� ���� ��� �ڵ��� ����(inst_table.txt)�� �о�
 *       ���� ��� ���̺�(inst_table)�� �����ϴ� �Լ��̴�.
 *
 *
 * �Ű� : ���� ��� ����
 * ��ȯ : �������� = 0 , ���� < 0
 * ���� : ���� ������� ������ �����Ӱ� �����Ѵ�. ���ô� ������ ����.
 *
 *	===============================================================================
 *		   | �̸� | ���� | ���� �ڵ� | ���۷����� ���� | \n |
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
	while (fgets(buffer, sizeof(buffer), file)) {	//inst_table.txt�� �� �پ� �о
		inst* _inst = (inst*)malloc(sizeof(inst));
		unsigned int temp_op;
		sscanf(buffer, "%s %x %d %d", &(_inst->str), &(temp_op), &(_inst->format), &(_inst->ops));
		//(�̸�\t���� �ڵ�\t����\t���۷����� ����) ���·� ����� ��ɾ� ������ inst ����ü ������ �����Ѵ�.
		_inst->op = (unsigned char)temp_op;
		inst_table[inst_index++] = _inst;
	}

	fclose(file);
	return 0;

	return errno;
}

/* ----------------------------------------------------------------------------------
 * ���� : ����� �� �ҽ��ڵ带 �о� �ҽ��ڵ� ���̺�(input_data)�� �����ϴ� �Լ��̴�.
 * �Ű� : ������� �ҽ����ϸ�
 * ��ȯ : �������� = 0 , ���� < 0
 * ���� : ���δ����� �����Ѵ�.
 *
 * ----------------------------------------------------------------------------------
 */
int init_input_file(char* input_file)
{
	FILE* file;
	int errno;

	/* add your code here */
	char path[1024];  // ��θ� ������ ����

	file = fopen(input_file, "r");
	if (file == NULL)
		return -1;

	line_num = 0;
	char buffer[LINE_LENGTH] = { 0 };
	while (fgets(buffer, sizeof(buffer), file)) {	//input.txt�� �� �پ� �о
		buffer[strlen(buffer) - 1] = 0;
		input_data[line_num++] = strdup(buffer);
		//���ڿ� �迭�� input_data�� ���ҷ� �����Ѵ�
	}

	fclose(file);
	return 0;

	return errno;
}



//����׿�, �Ľ��� ��ū�� ������ ���
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



//delimiter�� �������� ���ڿ� �ɰ��� �ɰ��� ���� ��Ʈ���� �迭�� ��ȯ
char** split_str(const char* str, size_t* length, const char* delim) {
	if (str == NULL) return NULL;
	char* tmpstr = strdup(str);	//strtok�� ���ڿ��� �ɰ��� ���� ���ڿ��� �Ѽյ� �� �����Ƿ� �ӽ� ���ڿ��� strtok ����
	size_t len = 0;
	char** splits = (char**)malloc(256 * sizeof(char*));

	char* split = strtok(tmpstr, delim);	//delim���� ������� �� ���� ��ūȭ
	while (split != NULL && len < 256) {	//�ִ� 1024���� ��ū�ޱ�
		splits[len++] = strdup(split);
		split = strtok(NULL, delim);
	}
	free(tmpstr);
	*length = len;
	return splits;
}



//���ڿ� �迭�� ��ü �޸� ����
void free_strarray(char** strarr, size_t len) {
	if (strarr == NULL) return;
	for (int i = 0; i < len; ++i) {
		if (strarr[i] == NULL)
			continue;
		free(strarr[i]);
	}
	free(strarr);
}



//C'EOF', X'05', =C'EOF' ���� ������� �Ľ�
char* parse_literal(const char* lit, size_t* len, int* equalflag) {
	if (lit == NULL) return NULL;
	char* current = lit;
	*equalflag = 0;
	if (*current == '=') {	//=���� �����ϴ� ���ͷ��� ó��
		*equalflag = 1;
		++current;
	}
	char* left = strstr(lit, "'");
	char* right = strrstr(lit, "'");
	if (left == NULL) return NULL;
	size_t litlen = right - left - 1;
	char* literal = (char*)malloc(litlen + 1);
	strncpy(literal, left + 1, litlen);	//����ǥ ������ ���� ����
	literal[litlen] = 0;
	*len = (*current == 'X') ? litlen / 2 : litlen;
	return literal;
}



//��ɾ��� �ǿ����ڰ� ���ͷ����� Ȯ���ϰ� ���ͷ��̸� �ӽ� ���̺� ����
void check_litoperand(const char* operand) {
	if (operand == NULL) return;
	size_t litlen = 0;
	int equalflag = 0;
	char* literal = parse_literal(operand, &litlen, &equalflag);
	if (literal && equalflag) {		//=���� �����ϴ� ���ͷ��̸� �ӽ� ���ͷ� ���̺� �߰�
		tmpliteral_table[tmplit_num].literal = strdup(literal);
		tmpliteral_table[tmplit_num].addr = locctr;
		tmplitlen_table[tmplit_num] = litlen;
		++tmplit_num;
	}
}



//�Լ� ���� ����
symbol search_symbol(const char* block, const char* sym);



//���̺��� �ּҸ� ã�� ��ü �ǿ����� ���� +-������ ����, ���� ���̺��̸� modification record�� �߰�
void accumlate_label(const char* block, const char* label, int* addr, int neg, int offset, int modsize) {
	if (block == NULL || label == NULL || addr == NULL) return;
	symbol sym = search_symbol(block, label);
	if (sym.addr >= 0) {
		*addr += neg ? -(sym.addr) : sym.addr;	//neg ��ȣ�� ���� �ּҰ����ٰ� ���ϱ�, ���� ���� ����
		return;
	}
	if (offset < 0)
		return;
	object_code* objcode = get_objinfo(block_name);
	objcode->mods[objcode->modcnt++] = (mod_record){ locctr + offset, modsize, neg, strdup(label), objcode->modcnt };
	//�ɺ� ���̺��� ��ã���� ������, �������� ���� �Ű������� �޾Ƽ� modification record�� �߰�
}



//���̺�� �̷���� �ǿ������� ��ü ���� ���
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
			label[i] = 0;	//+, - �ڸ��� �� �����ؼ� ���ڿ� �и�
			accumlate_label(block, start, &addr, negflag, offset, modsize);
			negflag = (opt == '-') ? 1 : 0;	//�����ڰ� -�� neg �� 1����
			start = label + i + 1;			//���� �ɺ� ���ڿ� ������
		}
	}
	if (start != NULL) {
		accumlate_label(block, start, &addr, negflag, offset, modsize);
	}
	return addr;
}



//��ɾ�� ��ū�� �м��ؼ� ������ų locctr ���� ���, ��� �������� ��쿡�� �߰��۾� ����
int get_locctr_add(inst instruction, token assemtok) {
	if ((assemtok.nixbpe >> 0) & 1) {
		return 4;
	}
	if (assemtok.operand[0] == NULL) {
		return instruction.format;
	}

	if (strcmp(instruction.str, "RESB") == 0) {	//RESB�� �ǿ����ڸ� ���� �Ľ��ؼ� ���� * 1 ��ȯ
		obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ 0, 0, locctr };
		int opdnum = string_to_int(assemtok.operand[0]);
		return opdnum * 1;
	}
	else if (strcmp(instruction.str, "RESW") == 0) {	//RESW�� �ǿ����ڸ� ���� �Ľ��ؼ� ���� * 3 ��ȯ
		obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ 0, 0, locctr };
		int opdnum = string_to_int(assemtok.operand[0]);
		return opdnum * 3;
	}
	else if (strcmp(instruction.str, "BYTE") == 0) {	//BYTE�� �ǿ����ڸ� ���ڿ� �Ľ��ؼ� 16������ ���� ��ȯ
		size_t litlen = 0;
		int equalflag = 0;
		char* literal = parse_literal(assemtok.operand[0], &litlen, &equalflag);
		unsigned int litcode = convert_literal(literal, &litlen);
		obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ litcode, litlen, locctr };

		return litlen;
	}
	else if (strcmp(instruction.str, "WORD") == 0) {	//WORD�� �ǿ����ڸ� ���� �Ľ� �õ��ϰ� �ȵǸ� �ɺ� �Ľ� ����
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
	else if (strcmp(instruction.str, "EQU") == 0) {	//EQU�� �ǿ����ڸ� ���� �Ľ� �õ��ϰ� �ȵǸ� �ɺ� �Ľ� ����
		size_t litlen = 0;
		int opdnum = string_to_int(assemtok.operand[0]);
		opdnum = (opdnum >= 0) ? opdnum : get_labelvalue(block_name, assemtok.operand[0], -1, 0);
		sector_symbol* blocksym = get_sectorsymbol(block_name);
		blocksym->sym_table[blocksym->length - 1].addr = opdnum;
		return 0;
	}
	return instruction.format;
}



//object code ����ü �ʱ�ȭ
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



//EOF�� 05 ���� ���ͷ��� 16���� �ڵ�� ��ȯ
unsigned int convert_literal(const char* literal, size_t* len) {
	if (literal == NULL || len == NULL) return NULL;

	char* endptr = NULL;
	unsigned int bincode = (unsigned int)strtoul(literal, &endptr, 16);	//���ڿ� �״�� 16���� ��ȯ �õ�(ex. "05" -> 0x05)
	if (*endptr == 0) {
		*len = strlen(literal) / 2;
		return bincode;
	}

	*len = 0;
	int shift = 0;
	bincode = 0;
	for (int i = strlen(literal) - 1; i >= 0; --i) {	//16���� ��ȯ �����ϸ� �� ������ 16���� �ƽ�Ű�ڵ� �߰�
		if (shift >= 4) break;
		bincode |= (literal[i] << (shift * 8));
		++(*len);
		++shift;
	}
	return bincode;
}



//��ū�� �����ڸ� �м��ؼ� �ʿ��ϸ� ���ο� ���α׷��� object code�� �߰��ϰ� ���̺��� �����Ѵٸ� �߰�
void add_sector_symbol(const char* op) {
	if (op == NULL) return;
	if (strcmp(op, "START") == 0 || strcmp(op, "CSECT") == 0) {	//���ο� Section ����
		if (block_name != NULL) {
			obj_infos[obj_num].length = locctr;
		}
		block_name = token_table[token_line]->label;

		int start = 0;
		if (strcmp(op, "START") == 0) {
			start = string_to_int(token_table[token_line]->operand[0]);	//START�� ������ġ �Ľ�
		}
		++obj_num;
		init_objcode();
		obj_infos[obj_num].start = start;
		obj_infos[obj_num].name = strdup(block_name);
		locctr = 0;
	}
	add_symbol(block_name, token_table[token_line]->label);	//���̺� �����ϸ� Section�� �ɺ� �߰�
}



//�ӽ� ���ͷ� ���̺� ����� ���ͷ��� ���ͷ� ���̺� ����
void ltorg(const char* op, int checkop) {
	if (checkop && op == NULL) return;
	if (checkop && strcmp(op, "LTORG") != 0) return;
	while (tmplit_comp < tmplit_num - 1) {	//�ӽ� ���ͷ� ���̺��� ������ ���ұ��� ����
		literal entry = tmpliteral_table[++tmplit_comp];
		int haslit = 0;
		for (int i = 0; i < literal_num; ++i) {
			if (strcmp(entry.literal, literal_table[i].literal) == 0) {	//���ͷ� ���̺� �߰��� ���ͷ��� �ߺ��Ǵ��� �˻�
				haslit = 1;
				break;
			}
		}
		if (haslit) continue;	//�ߺ����� ���� ���ͷ��̸� ���ͷ� ���̺� �߰�
		literal_table[literal_num].literal = strdup(entry.literal);
		literal_table[literal_num].addr = locctr;
		size_t litlen = 0;
		unsigned int litcode = convert_literal(entry.literal, &litlen);	//���ͷ� 16���� �ڵ� ��ȯ�ϰ� Section�� object code�� �߰�
		obj_infos[obj_num].bins[obj_infos[obj_num].bincnt++] = (binary){ litcode, litlen, locctr };
		locctr += tmplitlen_table[tmplit_comp];
		++literal_num;
	}
}



//�ǿ����ڸ� �м��ؼ� immediate, indirect ���� �ּ� ��� ����� ����
void check_ni(const char* operand) {
	if (operand == NULL) return NULL;
	if (operand[0] == '#') {	//immediate
		token_table[token_line]->nixbpe &= ~(1 << 5);
	}
	else if (operand[0] == '@') {	//indirect
		token_table[token_line]->nixbpe &= ~(1 << 4);
	}
}



//���̺� �Ľ��ϰ� ��ū�� ����, ���� -> 1, ���� -> 0
int parse_label(const char** lexes, size_t* idx, size_t lexlen) {
	if (lexes == NULL) return 0;
	if (*idx >= lexlen) return 0;
	token_table[token_line]->label = strdup(lexes[*idx]);
	++(*idx);
	return 1;
}



//������ �Ľ��ϰ� ��ū�� ����, ���� -> 1, ���� -> 0
int parse_operator(int opidx, size_t* idx, size_t lexlen) {
	if (opidx < 0) return 0;
	char opcodestr[3] = { 0 };
	snprintf(opcodestr, 3, "%02X", inst_table[opidx]->op);	//�� �ڸ� 16������ ���ڿ��� ����
	token_table[token_line]->opt = strdup(opcodestr);
	++(*idx);
	return 1;
}



//�ǿ����� �Ľ��ϰ� ��ū�� ����, ���� -> 1, ���� -> 0
int parse_operands(const char** lexes, size_t* idx, size_t lexlen, int opds) {
	if (lexes == NULL) return 0;
	if (opds <= 0) return 1;
	if (*idx >= lexlen && opds > 0) return 0;
	if (strcmp(lexes[*idx], "EXTDEF") == 0 || strcmp(lexes[*idx], "EXTREF") == 0) {
		++(*idx);
		return 1;
	}

	check_litoperand(lexes[*idx]);	//���ͷ� �ǿ��������� �˻�
	int flag = 1;
	char* tmpstr = strdup(lexes[*idx]);

	char* operand = strtok(tmpstr, ",");
	check_ni(operand);	//immdiate, indirect �˻�
	for (int i = 0; i < opds; ++i) {	//��ɾ��� ops ��ŭ �ǿ����� �Ľ�
		if (operand == NULL) {
			flag = 0;
			break;
		}
		token_table[token_line]->operand[i] = strdup(operand);
		operand = strtok(NULL, ",");	//��ǥ�� �������� �ǿ����� �и�
	}
	
	if (operand && strcmp(operand, "X") == 0) {	//������ �ǿ����ڿ� X�� ������ index addressing
		token_table[token_line]->nixbpe |= (1 << 3);
		flag = 1;
	}

	free(tmpstr);
	++(*idx);
	return flag;
}



//�ּ� �Ľ��ϰ� ��ū�� ����, ���� -> 1, ���� -> 0
int parse_comment(const char** lexes, size_t* idx, size_t lexlen) {
	if (lexes == NULL) return 0;
	if (*idx >= lexlen) return 1;

	char comment[100] = { 0 };
	strcpy(comment, lexes[*idx]);
	++(*idx);
	while (*idx < lexlen) {	//��ɾ� ������ �ּ����� �߰�
		strcat(comment, " ");
		strcat(comment, lexes[*idx]);
		++(*idx);
	}

	strcpy(token_table[token_line]->comment, comment);
	++(*idx);
	return 1;
}



//���ͷ� ���̺��� Ž���ؼ� �ش� ���ͷ� ����ü ��ȯ
literal search_literal(const char* lit) {
	if (lit == NULL) return (literal) { "", -1 };
	for (int i = 0; i < literal_num; ++i) {
		if (strcmp(lit, literal_table[i].literal) == 0) {
			return literal_table[i];
		}
	}
	return (literal) { "", -1 };
}



//�ɺ� ���̺��� Ž���ؼ� �ش� �ɺ� ����ü ��ȯ
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



//�����ڿ� �ǿ����ڸ� �м��ؼ� EXTDEF EXTREF ���̺� ����
void check_ext(const char* op, const char* opd) {
	if (op == NULL || opd == NULL) return;
	if (strcmp(op, "EXTDEF") && strcmp(op, "EXTREF")) return;
	size_t len = 0;
	char** opds = split_str(opd, &len, ",");
	if (strcmp(op, "EXTDEF") == 0) {	//EXTDEF ��� �ɺ� �߰�
		for (int i = 0; i < len; ++i) {
			const char* def = opds[i];
			obj_infos[obj_num].defs[obj_infos[obj_num].deflen++] = strdup(def);
		}
	}
	else if (strcmp(op, "EXTREF") == 0) {	//EXTREF ��� �ɺ� �߰�
		for (int i = 0; i < len; ++i) {
			const char* ref = opds[i];
			obj_infos[obj_num].refs[obj_infos[obj_num].reflen++] = strdup(ref);
		}
	}
	free_strarray(opds, len);
}



//token ����ü �ʱ�ȭ
void init_token() {
	token_table[token_line] = (token*)malloc(sizeof(token));
	token_table[token_line]->label = NULL;
	token_table[token_line]->opt = NULL;
	for (int i = 0; i < MAX_OPERAND; ++i) {
		token_table[token_line]->operand[i] = NULL;
	}
	strcpy(token_table[token_line]->comment, "");
	token_table[token_line]->nixbpe = 0x32; //0011 0010 ���� �ʱ�ȭ
}

/* ----------------------------------------------------------------------------------
 * ���� : �ҽ� �ڵ带 �о�� ��ū������ �м��ϰ� ��ū ���̺��� �ۼ��ϴ� �Լ��̴�.
 *        �н� 1�� ���� ȣ��ȴ�.
 * �Ű� : �Ľ��� ���ϴ� ���ڿ�
 * ��ȯ : �������� = 0 , ���� < 0
 * ���� : my_assembler ���α׷������� ���δ����� ��ū �� ������Ʈ ������ �ϰ� �ִ�.
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
	if (opidx < 0) {	//��ɾ��� ù �ܾ �����ڰ� �ƴϸ� ���̺���� �Ľ�
		flag = flag && parse_label(lexemes, &lexidx, lexlen);
	}
	//���ĺ��� ���ʴ�� ������, �ǿ�����, �ּ� �Ľ� ����
	opidx = search_opcode(lexemes[lexidx]);
	const char* opstr = lexemes[lexidx];
	flag = flag && parse_operator(opidx, &lexidx, lexlen);
	const char* opd = lexemes[lexidx];
	flag = flag && parse_operands(lexemes, &lexidx, lexlen, inst_table[opidx]->ops);
	flag = flag && parse_comment(lexemes, &lexidx, lexlen);
	ltorg(opstr, 1);	//LTORG ��ɾ����� Ȯ��
	add_sector_symbol(opstr);	//���ο� Section�̳� ���ο� �ɺ��� �߰��ؾ� �ϴ��� Ȯ��
	check_ext(opstr, opd);	//EXTREF, EXTDEF Ȯ��
	opstrs[token_line] = strdup(opstr);
	char nixbpe = token_table[token_line]->nixbpe;
	formats[token_line] = (nixbpe >> 0) & 1 ? 4 : inst_table[opidx]->format;	//e ��Ʈ �˻��ؼ� format ����

#ifdef DEBUG
	print_token();
#endif // DEBUG

	locctr += get_locctr_add(*inst_table[opidx], *token_table[token_line]);	//���� ��ɾ� �м��� ���� locctr ����
	++token_line;

	free_strarray(lexemes, lexlen);
	return flag - 1;
}



/* ----------------------------------------------------------------------------------
 * ���� : �Է� ���ڿ��� ���� �ڵ������� �˻��ϴ� �Լ��̴�.
 * �Ű� : ��ū ������ ���е� ���ڿ�
 * ��ȯ : �������� = ���� ���̺� �ε���, ���� < 0
 * ���� : ���� ��� ���̺��� Ư�� ��� �˻��Ͽ�, �ش� ��� ��ġ�� �ε����� ��ȯ�Ѵ�.
 *        '+JSUB'�� ���� ���ڿ��� ���� ó���� �����Ӱ� ó���Ѵ�.
 *
 * ----------------------------------------------------------------------------------
 */
int search_opcode(char* str)
{
	/* add your code here */
	if (str == NULL) return -1;
	const char* targetop = str;
	if (str[0] == '+') {	//4���� ��ɾ��� ��� '+'�� ���ڿ��� Ž�� ���, e bit�� 1�� ����, pc bit�� 0���� ����
		token_table[token_line]->nixbpe |= (1 << 0);
		token_table[token_line]->nixbpe &= ~(1 << 1);
		++targetop;
	}

	for (int i = 0; i < inst_index; ++i) {
		if (strcmp(targetop, inst_table[i]->str) != 0)	//��ɾ� ����ü �迭�� ��ȸ�ϸ鼭 ��ū�̶� ������ Ȯ��
			continue;

		if (str[0] == '+' && inst_table[i]->format != 3) {	//4���� ��ɾ��ε� ������ 3���� Ȯ��, �ƴϸ� ����
			return -1;
		}
		return i;
	}
	return -1;	//������ ��ɾ� ��ã���� ����
}

/* ----------------------------------------------------------------------------------
* ���� : ����� �ڵ带 ���� �н�1������ �����ϴ� �Լ��̴�.
*		   �н�1������..
*		   1. ���α׷� �ҽ��� ��ĵ�Ͽ� �ش��ϴ� ��ū������ �и��Ͽ� ���α׷� ���κ� ��ū
*		   ���̺��� �����Ѵ�.
*          2. ��ū ���̺��� token_parsing()�� ȣ���Ͽ� �����Ѵ�.
*          3. assem_pass2 �������� ����ϱ� ���� �ɺ����̺� �� ���ͷ� ���̺��� �����Ѵ�.
*
* �Ű� : ����
* ��ȯ : ���� ���� = 0 , ���� = < 0
* ���� : ���� �ʱ� ���������� ������ ���� �˻縦 ���� �ʰ� �Ѿ �����̴�.
*	  ���� ������ ���� �˻� ��ƾ�� �߰��ؾ� �Ѵ�.
*
* -----------------------------------------------------------------------------------
*/
static int assem_pass1(void)
{
	/* add your code here */

	/* input_data�� ���ڿ��� ���پ� �Է� �޾Ƽ�
	 * token_parsing()�� ȣ���Ͽ� _token�� ����
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

	make_opcode_output("output.txt");	//opcode�� ���� ������ �ִ� ������� ����
	return 0; 
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : �ҽ��ڵ� ��ɾ� �տ� OPCODE�� ��ϵ� �ڵ带 ���Ͽ� ����Ѵ�.
*        ������ NULL���� ���´ٸ� ���α׷��� ����� stdout���� ������
*        ȭ�鿡 ������ش�.
*        ������Ʈ 1������ ���ʿ��ϴ�.
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
	//	fprintf(fp, "%s\n", input_data[i]);	//opcode�� ������ ������� �� ������ output.txt�� �Է�
	//}
	//fclose(fp);
	return;
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ SYMBOL�� �ּҰ��� ����� TABLE�̴�.
* �Ű� : ������ ������Ʈ ���ϸ� Ȥ�� ���
* ��ȯ : ����
* ���� : ������ NULL���� ���´ٸ� ���α׷��� ����� stdout���� ������
*        ȭ�鿡 ������ش�.
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
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ LITERAL�� �ּҰ��� ����� TABLE�̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ������ NULL���� ���´ٸ� ���α׷��� ����� stdout���� ������
*        ȭ�鿡 ������ش�.
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



//�������ͺ� ��ȣ ���
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



//�����ڿ� nixbpe������ �ݿ��� 16���� �ڵ忡 �ǿ����� ���� ä���
unsigned int fill_operand(unsigned int binary, int idx) {
	if (formats[idx] == 2) {	//2���� ��ɾ�� �ǿ����ڸ� �������� ��ȣ�� ��ȯ
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
	else if (formats[idx] == 3 || formats[idx] == 4) {	//3, 4�����̸� �ɺ� Ž��, ���� ��ȯ, ���ͷ� Ž���� ����
		if (token_table[idx]->operand[0] == NULL) {
			token_table[idx]->nixbpe &= ~(1 << 1);
			return binary;
		}
		char* start = token_table[idx]->operand[0];
		int num = -1;
		if (*start == '@' || *start == '#') {
			++start;
		}
		symbol sym = search_symbol(block_name, start);	//���� �ɺ� Ž�� �õ�
		if (sym.addr >= 0) {
			num = sym.addr - (locctrs[idx] + formats[idx]);
			binary |= ((num & 0xFFF) << 0);
		}
		else if ((num = string_to_int(start)) >= 0) {	//���� ��ȯ �õ�
			token_table[idx]->nixbpe &= ~(1 << 1);
			binary |= (num << 0);
		}
		else {
			size_t litlen = 0;
			int equalflag = 0;
			char* litstr = parse_literal(start, &litlen, &equalflag);
			literal lit = search_literal(litstr);	//���ͷ� Ž�� �õ�
			free(litstr);
			if (lit.addr >= 0) {
				num = lit.addr - (locctrs[idx] + formats[idx]);
				binary |= ((num & 0xFFF) << 0);
			}
			else {	//+, -�� �̷���� �ɺ��� ��� �õ�, ���� �ɺ��̸� 0���� ä��� modification record �߰�
				num = get_labelvalue(block_name, start, 1, formats[idx] * 2 - 3);
				binary += num;
			}
		}
	}
	return binary;
}



/* ----------------------------------------------------------------------------------
* ���� : ����� �ڵ带 ���� �ڵ�� �ٲٱ� ���� �н�2 ������ �����ϴ� �Լ��̴�.
*		   �н� 2������ ���α׷��� ����� �ٲٴ� �۾��� ���� ������ ����ȴ�.
*		   ������ ���� �۾��� ����Ǿ� ����.
*		   1. ������ �ش� ����� ��ɾ ����� �ٲٴ� �۾��� �����Ѵ�.
* �Ű� : ����
* ��ȯ : �������� = 0, �����߻� = < 0
* ���� :
* -----------------------------------------------------------------------------------
*/
static int assem_pass2(void)
{
	/* add your code here */
	object_code* objinfo = NULL;
	for (int i = 0; i < token_line; ++i) {
		locctr = locctrs[i];
		token* tokent = token_table[i];
		if (strcmp(opstrs[i], "START") == 0 || strcmp(opstrs[i], "CSECT") == 0) {	//���� pass ���� Section �̸� ����
			block_name = tokent->label;
			objinfo = get_objinfo(block_name);	//���� pass ���� Section object code ���
			continue;
		}

		if (strcmp(tokent->opt, "FF") == 0) continue;

		unsigned int code = (unsigned int)strtoul(tokent->opt, NULL, 16);	//��ɾ� �ڵ� ��ȯ
		code = code << (formats[i] - 1) * 8;	//format ��ŭ ��Ʈ Ȯ���ϱ�
		code = fill_operand(code, i);	//�ǿ����� ��Ʈ ä���
		if (formats[i] >= 3) {	//3���� �̻��̸� nixbpe ä���
			unsigned char ni = (tokent->nixbpe >> 4) & 0xF;
			code |= (ni << (formats[i] - 1) * 8);
			unsigned char xbpe = (tokent->nixbpe >> 0) & 0xF;
			code |= (xbpe << (formats[i] - 1) * 8 - 4);
		}
		objinfo->bins[objinfo->bincnt++] = (binary){ code, formats[i], locctrs[i] };	//���� 16���� �ڵ� ����
	}
}



//locctr�� �������� ���α׷��� 16���� �ڵ� �����ϴ� �� �Լ�
int compare_locctr(const void* a, const void* b) {
	binary* bin1 = (binary*)a;
	binary* bin2 = (binary*)b;
	if(bin1->start != bin2->start)
		return bin1->start - bin2->start;
	return bin1->len - bin2->len;
}


//modification record�� ������ġ�� �������� ���α׷��� mod record�� �����ϴ� �� �Լ�
int compare_start(const void* a, const void* b) {
	mod_record* mod1 = (mod_record*)a;
	mod_record* mod2 = (mod_record*)b;
	if (mod1->pos == mod2->pos)
		return mod1->idx - mod2->idx;
	return mod1->pos - mod2->pos;
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ object code�̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ������ NULL���� ���´ٸ� ���α׷��� ����� stdout���� ������
*        ȭ�鿡 ������ش�.
*        ������ �־��� ��� ����� ������ �����ؾ� �Ѵ�.
*        ���������� �� ���� ������ ���� ���� Ȥ�� ���� ������ ���̴� ����Ѵ�.
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

		//��� ���� ���
		memset(buffer, 0, MAX_OPJECTS);
		snprintf(buffer, MAX_OPJECTS, "H%-6s%06X%06X\n", obj_infos[i].name, obj_infos[i].start, obj_infos[i].length);
		fprintf(fp, buffer);

		//EXTDEF ���� ���
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

		//EXTREF ���� ���
		memset(buffer, 0, MAX_OPJECTS);
		for (int j = 0; j < obj_infos[i].reflen; ++j) {
			snprintf(buffer + strlen(buffer), MAX_OPJECTS, "%-6s", obj_infos[i].refs[j]);
		}
		if (strcmp(buffer, "") != 0) {
			fprintf(fp, "R%s\n", buffer);
		}

		//TEXT ���� ���
		memset(buffer, 0, MAX_OPJECTS);
		int total_len = 0;
		int start_loc = 0;
		for (int j = 0; j < obj_infos[i].bincnt; ++j) {
			if (total_len + obj_infos[i].bins[j].len > 30 || obj_infos[i].bins[j].len == 0) {
				//���̰� 30 ����Ʈ �ʰ��ų� �������� �ּҷ� ������� ������ �ٹٲ�
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
			//�̸� ������ �ڵ� ����Ʈ ���̸�ŭ ��� ���� ����
			total_len += obj_infos[i].bins[j].len;
		}
		if (strcmp(buffer, "") != 0) {
			fprintf(fp, "T%06X%02X%s\n", start_loc, total_len, buffer);
		}

		//modification record ���
		memset(buffer, 0, MAX_OPJECTS);
		for (int j = 0; j < obj_infos[i].modcnt; ++j) {
			qsort(obj_infos[i].mods, obj_infos[i].modcnt, sizeof(mod_record), compare_start);
			char opt = obj_infos[i].mods[j].neg ? '-' : '+';
			snprintf(buffer, MAX_OPJECTS, "%06X%02X%c%s", obj_infos[i].mods[j].pos, obj_infos[i].mods[j].bytes, opt, obj_infos[i].mods[j].label);
			fprintf(fp, "M%s\n", buffer);
		}

		//END ���� ���
		memset(buffer, 0, MAX_OPJECTS);
		if (i == 0) {
			snprintf(buffer, MAX_OPJECTS, "%06X", 0);
		}
		fprintf(fp, "E%s\n\n", buffer);
	}
}