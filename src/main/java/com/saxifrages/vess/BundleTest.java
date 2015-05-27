package com.saxifrages.vess;

import com.essbase.server.framework.EssServerFramework;
import com.hyperion.dsf.server.framework.DsfResourceBundle;

public class BundleTest {

	public static void main(String[] args) {
		   StringBuffer localStringBuffer = new StringBuffer();
		    localStringBuffer.append(DsfResourceBundle.sm_getString("AAAA" + "Essbase(r)_Deployment_Services_-_Release") + " ");
		    Package localPackage = EssServerFramework.class.getPackage();
		    String str1 = localPackage.getImplementationVersion();
		    localStringBuffer.append(str1);
		    String str2 = localPackage.getImplementationTitle();
		    if (str2 != null) {
		      localStringBuffer.append(", ").append(str2);
		    }
		    localStringBuffer.append("\n");
		    localStringBuffer.append("Copyright (c) 1991, 2013 Oracle and / or its affiliates. All rights reserved.");
		    
		    System.out.println("Final string: " + localStringBuffer.toString());

	}

}
