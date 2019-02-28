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

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Singleton
public class Conversion {

    public static Map<String, String> symbolMap;
    public static Map<String, Character> binToAsciiMap;


    public Conversion(){
        buildSymbolMap();
        buildBinToAsciiMap();
    }


    public String getBinaryForSymbol(char c) throws Exception {
        return symbolMap.get("" + c);
    }


    // decoder for binary6-strings
    public String getAsciiStringFromBinaryString(String binary) {

        StringBuilder builder = new StringBuilder();
        if (binary == null || binary.length() < 1) {
            return "";
        }
        String parts[] = splitToNChar(binary, 6);
        int len = parts.length;
        if (len < 1) {
            return "";
        }
        for (int pos = 0; pos < len; pos++) {

            String bin = parts[pos];
            Character chr = binToAsciiMap.get(bin);
            if(chr != null){
                builder.append(chr);
            }
        }
        return removeTrailingSnabelA(builder.toString());
    }

    String removeTrailingSnabelA(String s) {
        int index;
        for (index = s.length() - 1; index >= 0; index--) {
            if (s.charAt(index) != '@') {
                break;
            }
        }
        return s.substring(0, index + 1);
    }


    /**
     * Split text into n number of characters.
     *
     * @param text the text to be split.
     * @param size the split size.
     * @return an array of the split text.
     */
    private String[] splitToNChar(String text, int size) {
        List<String> parts = new ArrayList<>();

        int length = text.length();
        for (int i = 0; i < length; i += size) {
            parts.add(text.substring(i, Math.min(length, i + size)));
        }
        return parts.toArray(new String[0]);
    }

    private void buildBinToAsciiMap() {
        // Table 3 sixbit ascii
        // http://catb.org/gpsd/AIVDM.html

        binToAsciiMap = new TreeMap<>();

        binToAsciiMap.put("000000",'@');
        binToAsciiMap.put("000001",'A');
        binToAsciiMap.put("000010",'B');
        binToAsciiMap.put("000011",'C');
        binToAsciiMap.put("000100",'D');
        binToAsciiMap.put("000101",'E');
        binToAsciiMap.put("000110",'F');
        binToAsciiMap.put("000111",'G');
        binToAsciiMap.put("001000",'H');
        binToAsciiMap.put("001001",'I');
        binToAsciiMap.put("001010",'J');
        binToAsciiMap.put("001011",'K');
        binToAsciiMap.put("001100",'L');
        binToAsciiMap.put("001101",'M');
        binToAsciiMap.put("001110",'N');
        binToAsciiMap.put("001111",'O');


        binToAsciiMap.put("010000",'P');
        binToAsciiMap.put("010001",'Q');
        binToAsciiMap.put("010010",'R');
        binToAsciiMap.put("010011",'S');
        binToAsciiMap.put("010100",'T');
        binToAsciiMap.put("010101",'U');
        binToAsciiMap.put("010110",'V');
        binToAsciiMap.put("010111",'W');
        binToAsciiMap.put("011000",'X');
        binToAsciiMap.put("011001",'Y');
        binToAsciiMap.put("011010",'Z');
        binToAsciiMap.put("011011",'[');
        binToAsciiMap.put("011100",'\\');
        binToAsciiMap.put("011101",']');
        binToAsciiMap.put("011110",'^');
        binToAsciiMap.put("011111",'_');

        binToAsciiMap.put("100000",' ');
        binToAsciiMap.put("100001",'!');
        binToAsciiMap.put("100010",'\"');
        binToAsciiMap.put("100011",'#');
        binToAsciiMap.put("100100",'$');
        binToAsciiMap.put("100101",'%');
        binToAsciiMap.put("100110",'&');
        binToAsciiMap.put("100111",'\'');
        binToAsciiMap.put("101000",'(');
        binToAsciiMap.put("101001",')');
        binToAsciiMap.put("101010",'*');
        binToAsciiMap.put("101011",'+');
        binToAsciiMap.put("101100",',');
        binToAsciiMap.put("101101",'-');
        binToAsciiMap.put("101110",'.');
        binToAsciiMap.put("101111",'/');

        binToAsciiMap.put("110000",'0');
        binToAsciiMap.put("110001",'1');
        binToAsciiMap.put("110010",'2');
        binToAsciiMap.put("110011",'3');
        binToAsciiMap.put("110100",'4');
        binToAsciiMap.put("110101",'5');
        binToAsciiMap.put("110110",'6');
        binToAsciiMap.put("110111",'7');
        binToAsciiMap.put("111000",'8');
        binToAsciiMap.put("111001",'9');
        binToAsciiMap.put("111010",':');
        binToAsciiMap.put("111011",';');
        binToAsciiMap.put("111100",'<');
        binToAsciiMap.put("111101",'=');
        binToAsciiMap.put("111110",'>');
        binToAsciiMap.put("111111",'?');

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
    }

}