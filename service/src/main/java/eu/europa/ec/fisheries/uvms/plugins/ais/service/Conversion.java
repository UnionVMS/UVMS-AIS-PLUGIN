/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package eu.europa.ec.fisheries.uvms.plugins.ais.service;

import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;

@Singleton
public class Conversion {

    public static Map<String, String> symbolMap;
    private static Map<String, String> binaryToSymbolMap;


    @PostConstruct
    public void init() {
        buildSymbolMap();
    }

    public String getBinaryForSymbol(char c) throws Exception {
        return symbolMap.get("" + c);
    }

    public String getSymbolForBinary(String binary) throws Exception {
        if((binary == null) || (binary.length() != 6)) throw new Exception("binary must be 6 in length");
        return binaryToSymbolMap.get(binary);
    }


    private void buildSymbolMap() {
        symbolMap = new TreeMap<>();
        symbolMap.put("0", "000000");
        symbolMap.put("1", "000001");
        symbolMap.put("2", "000010");
        symbolMap.put("3", "000011");
        symbolMap.put("4", "000100");
        symbolMap.put("5", "000101");
        symbolMap.put("6", "000110");
        symbolMap.put("7", "000111");
        symbolMap.put("8", "001000");
        symbolMap.put("9", "001001");
        symbolMap.put(":", "001010");
        symbolMap.put(";", "001011");
        symbolMap.put("<", "001100");
        symbolMap.put("=", "001101");
        symbolMap.put(">", "001110");
        symbolMap.put("?", "001111");
        symbolMap.put("@", "010000");
        symbolMap.put("A", "010001");
        symbolMap.put("B", "010010");
        symbolMap.put("C", "010011");
        symbolMap.put("D", "010100");
        symbolMap.put("E", "010101");
        symbolMap.put("F", "010110");
        symbolMap.put("G", "010111");
        symbolMap.put("H", "011000");
        symbolMap.put("I", "011001");
        symbolMap.put("J", "011010");
        symbolMap.put("K", "011011");
        symbolMap.put("L", "011100");
        symbolMap.put("M", "011101");
        symbolMap.put("N", "011110");
        symbolMap.put("O", "011111");
        symbolMap.put("P", "100000");
        symbolMap.put("Q", "100001");
        symbolMap.put("R", "100010");
        symbolMap.put("S", "100011");
        symbolMap.put("T", "100100");
        symbolMap.put("U", "100101");
        symbolMap.put("V", "100110");
        symbolMap.put("W", "100111");
        symbolMap.put("`", "101000");
        symbolMap.put("a", "101001");
        symbolMap.put("b", "101010");
        symbolMap.put("c", "101011");
        symbolMap.put("d", "101100");
        symbolMap.put("e", "101101");
        symbolMap.put("f", "101110");
        symbolMap.put("g", "101111");
        symbolMap.put("h", "110000");
        symbolMap.put("i", "110001");
        symbolMap.put("j", "110010");
        symbolMap.put("k", "110011");
        symbolMap.put("l", "110100");
        symbolMap.put("m", "110101");
        symbolMap.put("n", "110110");
        symbolMap.put("o", "110111");
        symbolMap.put("p", "111000");
        symbolMap.put("q", "111001");
        symbolMap.put("r", "111010");
        symbolMap.put("s", "111011");
        symbolMap.put("t", "111100");
        symbolMap.put("u", "111101");
        symbolMap.put("v", "111110");
        symbolMap.put("w", "111111");




        binaryToSymbolMap = new TreeMap<>();
        for (Map.Entry<String, String> entry : symbolMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            binaryToSymbolMap.put(value, key);
        }



    }

}