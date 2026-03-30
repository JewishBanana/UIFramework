package com.github.jewishbanana.uiframework.items;

public class StoredField<T> {
	
	protected T value;
	protected boolean persists;
	
	protected StoredField(T value, boolean persists) {
		this.value = value;
		this.persists = persists;
	}
	public T getValue() {
		return value;
	}
	public void setValue(T setting) {
		this.value = setting;
	}
	public boolean doesPersist() {
		return persists;
	}
	public void setPersist(boolean persist) {
		this.persists = persist;
	}
}
