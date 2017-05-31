package com.huawei.esdk.uc;

/**
 * 方法调用返回值
 * */
public enum FuncResultCode {
	
    RESULT_SUCCESS(0), //调用成功
    RESULT_NULL_PARAM(-1); //参数存在空指针
    
    private final int _value;
    
    private FuncResultCode(int value){
    	this._value = value;
    }
    
    public int value(){
    	return this._value;
    }
    
}
