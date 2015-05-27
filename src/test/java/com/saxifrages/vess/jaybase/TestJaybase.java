package com.saxifrages.vess.jaybase;

import org.jaybase.core.JEssbaseBuilder;
import org.jaybase.session.JEssbase;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapServer;

public class TestJaybase {

	public static JEssbase make() {
		JEssbase essbase = new JEssbaseBuilder()
			.addVariable("CurMonth", "January")
			.addVariable("CurYear", "FY15")
			.build();
		
		return essbase;
	}
	
	public static void main(String[] args) throws EssException {
		JEssbase essbase = make();
		IEssOlapServer server = essbase.signOn(null, null, false, null, null, null);
		
		String value = server.getSubstitutionVariableValue("CurMonth");
		System.out.println("Value: " + value);
		
		String[][] variables = server.getSubstitutionVariables();
		System.out.println("End");
		
	}
	
}
