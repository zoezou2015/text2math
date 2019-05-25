package com.statnlp.commons;

import com.statnlp.commons.StringUtil;

public class StringUtilTest {

	public static void main(String args[]){
		String input = "<sentence><cons lex=\"adenovirus_(Ad)_infection\" sem=\"G#other_name\"><cons lex=\"adenovirus\" sem=\"G#virus\">Adenovirus</cons> (Ad)-infection</cons>and <cons lex=\"E1A_transfection\" sem=\"G#other_name\"><cons lex=\"E1A\" sem=\"G#protein_molecule\">E1A</cons> transfection</cons> were used to model changes in susceptibility to <cons lex=\"NK_cell_killing\" sem=\"G#other_name\">NK cell killing</cons> caused by transient vs stable <cons lex=\"E1A_expression\" sem=\"G#other_name\"><cons lex=\"E1A\" sem=\"G#protein_molecule\">E1A</cons> expression</cons> in <cons lex=\"human_cell\" sem=\"G#cell_type\">human cells</cons>.</sentence>";
		String output = StringUtil.stripXMLTags(input);
		System.err.println(output);
	}

}
