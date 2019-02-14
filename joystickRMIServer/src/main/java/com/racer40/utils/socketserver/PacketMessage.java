/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.racer40.utils.socketserver;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author Kareem Moustafa
 */
public class PacketMessage extends LinkedHashMap<String, String> implements Serializable {
	private static final long serialVersionUID = 1L;

	public PacketMessage() {
	}

	public void setMap(Map<String, String> map) {
		this.clear();
		Iterator it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
			this.put(pair.getKey(), pair.getValue());
		}
	}

	@Override
	public String toString() {
		String msg = "";
		for (String key : this.keySet()) {
			msg += key + " : " + this.get(key) + "\n";
		}
		return msg;
	}

}