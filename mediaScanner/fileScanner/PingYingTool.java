package com.adayo.mediaScanner.fileScanner;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import android.util.Log;

public class PingYingTool {
	private static final String TAG = "PingYingTool";
	private static HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();  
	static{
 	    format.setCaseType(HanyuPinyinCaseType.UPPERCASE);  
	    format.setToneType(HanyuPinyinToneType.WITHOUT_TONE); 
	}
	
	public static String parseString(String str) {
        if(str==null)
        	return null;
		char[] chars = new char[str.length()];
		for (int i = 0; i < str.length(); i++) {
			chars[i] = getFirstChar(str.charAt(i));
		}
		return new String(chars);
	}
	
	private static char getFirstChar(char c){     
	    try{
			String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(c, format);
            if(pinyin!=null){
            	return pinyin[0].charAt(0);
            }else {
				return c;
			}
		} catch (BadHanyuPinyinOutputFormatCombination e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return c;
		} 
	}
}
