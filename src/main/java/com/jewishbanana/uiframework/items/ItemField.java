package com.jewishbanana.uiframework.items;

public class ItemField {
	
	private String lore;
	private int setting;
	
	public ItemField(int value) {
		this.setting = value;
	}
	public ItemField(int value, String lore) {
		this.setting = value;
		this.lore = lore;
	}
	public String getLore() {
		return lore;
	}
	public void setLore(String lore) {
		this.lore = lore;
	}
	public int getSetting() {
		return setting;
	}
	public void setSetting(int setting) {
		this.setting = setting;
	}
}
