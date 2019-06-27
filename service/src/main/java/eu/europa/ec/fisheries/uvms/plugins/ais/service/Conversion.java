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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Singleton
public class Conversion {

    private static final Logger LOG = LoggerFactory.getLogger(Conversion.class);

    public static Map<String, String> symbolMap;
    public static Map<String, Character> binToAsciiMap;
    public static Map<String, CountryCode> countryCodeMap;
    public static Map<Integer, String> shipTypeMap;


    public Conversion(){
        buildSymbolMap();
        buildBinToAsciiMap();
        buildCountryCodeMap();
        buildShiptypeMap();
    }

    private void buildShiptypeMap() {
        shipTypeMap = new TreeMap<>();

        shipTypeMap.put(0,"Not available (default)");
        shipTypeMap.put(1-19,"Reserved for future use");
        shipTypeMap.put(20,"Wing in ground (WIG) all ships of this type");
        shipTypeMap.put(21,"Wing in ground (WIG) Hazardous category A");
        shipTypeMap.put(22,"Wing in ground (WIG) Hazardous category B");
        shipTypeMap.put(23,"Wing in ground (WIG) Hazardous category C");
        shipTypeMap.put(24,"Wing in ground (WIG) Hazardous category D");
        shipTypeMap.put(25,"Wing in ground (WIG) Reserved for future use");
        shipTypeMap.put(26,"Wing in ground (WIG) Reserved for future use");
        shipTypeMap.put(27,"Wing in ground (WIG) Reserved for future use");
        shipTypeMap.put(28,"Wing in ground (WIG) Reserved for future use");
        shipTypeMap.put(29,"Wing in ground (WIG) Reserved for future use");
        shipTypeMap.put(30,"Fishing");
        shipTypeMap.put(31,"Towing");
        shipTypeMap.put(32,"Towing: length exceeds 200m or breadth exceeds 25m");
        shipTypeMap.put(33,"Dredging or underwater ops");
        shipTypeMap.put(34,"Diving ops");
        shipTypeMap.put(35,"Military ops");
        shipTypeMap.put(36,"Sailing");
        shipTypeMap.put(37,"Pleasure Craft");
        shipTypeMap.put(38,"Reserved");
        shipTypeMap.put(39,"Reserved");
        shipTypeMap.put(40,"High speed craft (HSC) all ships of this type");
        shipTypeMap.put(41,"High speed craft (HSC) Hazardous category A");
        shipTypeMap.put(42,"High speed craft (HSC) Hazardous category B");
        shipTypeMap.put(43,"High speed craft (HSC) Hazardous category C");
        shipTypeMap.put(44,"High speed craft (HSC) Hazardous category D");
        shipTypeMap.put(45,"High speed craft (HSC) Reserved for future use");
        shipTypeMap.put(46,"High speed craft (HSC) Reserved for future use");
        shipTypeMap.put(47,"High speed craft (HSC) Reserved for future use");
        shipTypeMap.put(48,"High speed craft (HSC) Reserved for future use");
        shipTypeMap.put(49,"High speed craft (HSC) No additional information");
        shipTypeMap.put(50,"Pilot Vessel");
        shipTypeMap.put(51,"Search and Rescue vessel");
        shipTypeMap.put(52,"Tug");
        shipTypeMap.put(53,"Port Tender");
        shipTypeMap.put(54,"Anti-pollution equipment");
        shipTypeMap.put(55,"Law Enforcement");
        shipTypeMap.put(56,"Spare - Local Vessel");
        shipTypeMap.put(57,"Spare - Local Vessel");
        shipTypeMap.put(58,"Medical Transport");
        shipTypeMap.put(59,"Noncombatant ship according to RR Resolution No. 18");
        shipTypeMap.put(60,"Passenger all ships of this type");
        shipTypeMap.put(61,"Passenger Hazardous category A");
        shipTypeMap.put(62,"Passenger Hazardous category B");
        shipTypeMap.put(63,"Passenger Hazardous category C");
        shipTypeMap.put(64,"Passenger Hazardous category D");
        shipTypeMap.put(65,"Passenger Reserved for future use");
        shipTypeMap.put(66,"Passenger Reserved for future use");
        shipTypeMap.put(67,"Passenger Reserved for future use");
        shipTypeMap.put(68,"Passenger Reserved for future use");
        shipTypeMap.put(69,"Passenger No additional information");
        shipTypeMap.put(70,"Cargo all ships of this type");
        shipTypeMap.put(71,"Cargo Hazardous category A");
        shipTypeMap.put(72,"Cargo Hazardous category B");
        shipTypeMap.put(73,"Cargo Hazardous category C");
        shipTypeMap.put(74,"Cargo Hazardous category D");
        shipTypeMap.put(75,"Cargo Reserved for future use");
        shipTypeMap.put(76,"Cargo Reserved for future use");
        shipTypeMap.put(77,"Cargo Reserved for future use");
        shipTypeMap.put(78,"Cargo Reserved for future use");
        shipTypeMap.put(79,"Cargo No additional information");
        shipTypeMap.put(80,"Tanker all ships of this type");
        shipTypeMap.put(81,"Tanker Hazardous category A");
        shipTypeMap.put(82,"Tanker Hazardous category B");
        shipTypeMap.put(83,"Tanker Hazardous category C");
        shipTypeMap.put(84,"Tanker Hazardous category D");
        shipTypeMap.put(85,"Tanker Reserved for future use");
        shipTypeMap.put(86,"Tanker Reserved for future use");
        shipTypeMap.put(87,"Tanker Reserved for future use");
        shipTypeMap.put(88,"Tanker Reserved for future use");
        shipTypeMap.put(89,"Tanker No additional information");
        shipTypeMap.put(90,"Other Type all ships of this type");
        shipTypeMap.put(91,"Other Type Hazardous category A");
        shipTypeMap.put(92,"Other Type Hazardous category B");
        shipTypeMap.put(93,"Other Type Hazardous category C");
        shipTypeMap.put(94,"Other Type Hazardous category D");
        shipTypeMap.put(95,"Other Type Reserved for future use");
        shipTypeMap.put(96,"Other Type Reserved for future use");
        shipTypeMap.put(97,"Other Type Reserved for future use");
        shipTypeMap.put(98,"Other Type Reserved for future use");
        shipTypeMap.put(99,"Other Type no additional information");
    }

    private void buildCountryCodeMap() {
        countryCodeMap = new TreeMap<>();

        /* This mapper is taken from: http://www.vtexplorer.com/mmsi-mid-codes-en/ */ 
        countryCodeMap.put("201",new CountryCode("201","Albania","AL","ALB"));
        countryCodeMap.put("202",new CountryCode("202","Andorra","AD","AND"));
        countryCodeMap.put("203",new CountryCode("203","Austria","AT","AUT"));
        countryCodeMap.put("204",new CountryCode("204","Portugal","PT","PRT"));
        countryCodeMap.put("205",new CountryCode("205","Belgium","BE","BEL"));
        countryCodeMap.put("206",new CountryCode("206","Belarus","BY","BLR"));
        countryCodeMap.put("207",new CountryCode("207","Bulgaria","BG","BGR"));
        countryCodeMap.put("208",new CountryCode("208","Vatican","VA","VAT"));
        countryCodeMap.put("209",new CountryCode("209","Cyprus","CY","CYP"));
        countryCodeMap.put("210",new CountryCode("210","Cyprus","CY","CYP"));
        countryCodeMap.put("211",new CountryCode("211","Germany","DE","DEU"));
        countryCodeMap.put("212",new CountryCode("212","Cyprus","CY","CYP"));
        countryCodeMap.put("213",new CountryCode("213","Georgia","GE","GEO"));
        countryCodeMap.put("214",new CountryCode("214","Moldova","MD","MDA"));
        countryCodeMap.put("215",new CountryCode("215","Malta","MT","MLT"));
        countryCodeMap.put("216",new CountryCode("216","Armenia","ZZ","ARM"));
        countryCodeMap.put("218",new CountryCode("218","Germany","DE","DEU"));
        countryCodeMap.put("219",new CountryCode("219","Denmark","DK","DNK"));
        countryCodeMap.put("220",new CountryCode("220","Denmark","DK","DNK"));
        countryCodeMap.put("224",new CountryCode("224","Spain","ES","ESP"));
        countryCodeMap.put("225",new CountryCode("225","Spain","ES","ESP"));
        countryCodeMap.put("226",new CountryCode("226","France","FR","FRA"));
        countryCodeMap.put("227",new CountryCode("227","France","FR","FRA"));
        countryCodeMap.put("228",new CountryCode("228","France","FR","FRA"));
        countryCodeMap.put("229",new CountryCode("229","Malta","MT","MLT"));
        countryCodeMap.put("230",new CountryCode("230","Finland","FI","FIN"));
        countryCodeMap.put("231",new CountryCode("231","Faroe Is","FO","FRO"));
        countryCodeMap.put("232",new CountryCode("232","United Kingdom","GB","GBR"));
        countryCodeMap.put("233",new CountryCode("233","United Kingdom","GB","GBR"));
        countryCodeMap.put("234",new CountryCode("234","United Kingdom","GB","GBR"));
        countryCodeMap.put("235",new CountryCode("235","United Kingdom","GB","GBR"));
        countryCodeMap.put("236",new CountryCode("236","Gibraltar","GI","GIB"));
        countryCodeMap.put("237",new CountryCode("237","Greece","GR","GRC"));
        countryCodeMap.put("238",new CountryCode("238","Croatia","HR","HRV"));
        countryCodeMap.put("239",new CountryCode("239","Greece","GR","GRC"));
        countryCodeMap.put("240",new CountryCode("240","Greece","GR","GRC"));
        countryCodeMap.put("241",new CountryCode("241","Greece","GR","GRC"));
        countryCodeMap.put("242",new CountryCode("242","Morocco","MA","MAR"));
        countryCodeMap.put("243",new CountryCode("243","Hungary","HU","HUN"));
        countryCodeMap.put("244",new CountryCode("244","Netherlands","NL","NLD"));
        countryCodeMap.put("245",new CountryCode("245","Netherlands","NL","NLD"));
        countryCodeMap.put("246",new CountryCode("246","Netherlands","NL","NLD"));
        countryCodeMap.put("247",new CountryCode("247","Italy","IT","ITA"));
        countryCodeMap.put("248",new CountryCode("248","Malta","MT","MLT"));
        countryCodeMap.put("249",new CountryCode("249","Malta","MT","MLT"));
        countryCodeMap.put("250",new CountryCode("250","Ireland","IE","IRL"));
        countryCodeMap.put("251",new CountryCode("251","Iceland","IS","ISL"));
        countryCodeMap.put("252",new CountryCode("252","Liechtenstein","LI","LIE"));
        countryCodeMap.put("253",new CountryCode("253","Luxembourg","LU","LUX"));
        countryCodeMap.put("254",new CountryCode("254","Monaco","MC","MCO"));
        countryCodeMap.put("255",new CountryCode("255","Portugal","PT","PRT"));
        countryCodeMap.put("256",new CountryCode("256","Malta","MT","MLT"));
        countryCodeMap.put("257",new CountryCode("257","Norway","NO","NOR"));
        countryCodeMap.put("258",new CountryCode("258","Norway","NO","NOR"));
        countryCodeMap.put("259",new CountryCode("259","Norway","NO","NOR"));
        countryCodeMap.put("261",new CountryCode("261","Poland","PL","POL"));
        countryCodeMap.put("262",new CountryCode("262","Montenegro","ME","MNE"));
        countryCodeMap.put("263",new CountryCode("263","Portugal","PT","PRT"));
        countryCodeMap.put("264",new CountryCode("264","Romania","RO","ROU"));
        countryCodeMap.put("265",new CountryCode("265","Sweden","SE","SWE"));
        countryCodeMap.put("266",new CountryCode("266","Sweden","SE","SWE"));
        countryCodeMap.put("267",new CountryCode("267","Slovakia","SK","SVK"));
        countryCodeMap.put("268",new CountryCode("268","San Marino","SM","SMR"));
        countryCodeMap.put("269",new CountryCode("269","Switzerland","CH","CHE"));
        countryCodeMap.put("270",new CountryCode("270","Czech Republic","CZ","CZE"));
        countryCodeMap.put("271",new CountryCode("271","Turkey","TR","TUR"));
        countryCodeMap.put("272",new CountryCode("272","Ukraine","UA","UKR"));
        countryCodeMap.put("273",new CountryCode("273","Russia","RU","RUS"));
        countryCodeMap.put("274",new CountryCode("274","FYR Macedonia","MK","MKD"));
        countryCodeMap.put("275",new CountryCode("275","Latvia","LV","LVA"));
        countryCodeMap.put("276",new CountryCode("276","Estonia","EE","EST"));
        countryCodeMap.put("277",new CountryCode("277","Lithuania","LT","LTU"));
        countryCodeMap.put("278",new CountryCode("278","Slovenia","SI","SVN"));
        countryCodeMap.put("279",new CountryCode("279","Serbia","RS","SRB"));
        countryCodeMap.put("301",new CountryCode("301","Anguilla","AI","AIA"));
        countryCodeMap.put("303",new CountryCode("303","USA","US","USA"));
        countryCodeMap.put("304",new CountryCode("304","Antigua Barbuda","AG","ATG"));
        countryCodeMap.put("305",new CountryCode("305","Antigua Barbuda","AG","ATG"));
        countryCodeMap.put("306",new CountryCode("306","Curacao","CW","???"));
        countryCodeMap.put("307",new CountryCode("307","Aruba","AW","ABW"));
        countryCodeMap.put("308",new CountryCode("308","Bahamas","BS","BHS"));
        countryCodeMap.put("309",new CountryCode("309","Bahamas","BS","BHS"));
        countryCodeMap.put("310",new CountryCode("310","Bermuda","BM","BMU"));
        countryCodeMap.put("311",new CountryCode("311","Bahamas","BS","BHS"));
        countryCodeMap.put("312",new CountryCode("312","Belize","BZ","BLZ"));
        countryCodeMap.put("314",new CountryCode("314","Barbados","BB","BRB"));
        countryCodeMap.put("316",new CountryCode("316","Canada","CA","CAN"));
        countryCodeMap.put("319",new CountryCode("319","Cayman Is","KY","CYM"));
        countryCodeMap.put("321",new CountryCode("321","Costa Rica","CR","CRI"));
        countryCodeMap.put("323",new CountryCode("323","Cuba","CU","CUB"));
        countryCodeMap.put("325",new CountryCode("325","Dominica","DM","DMA"));
        countryCodeMap.put("327",new CountryCode("327","Dominican Rep","DO","DOM"));
        countryCodeMap.put("329",new CountryCode("329","Guadeloupe","GP","GLP"));
        countryCodeMap.put("330",new CountryCode("330","Grenada","GD","GRD"));
        countryCodeMap.put("331",new CountryCode("331","Greenland","GL","GRL"));
        countryCodeMap.put("332",new CountryCode("332","Guatemala","GT","GTM"));
        countryCodeMap.put("334",new CountryCode("334","Honduras","HN","HND"));
        countryCodeMap.put("336",new CountryCode("336","Haiti","HT","HTI"));
        countryCodeMap.put("338",new CountryCode("338","USA","US","USA"));
        countryCodeMap.put("339",new CountryCode("339","Jamaica","JM","JAM"));
        countryCodeMap.put("341",new CountryCode("341","St Kitts Nevis","KN","KNA"));
        countryCodeMap.put("343",new CountryCode("343","St Lucia","LC","LCA"));
        countryCodeMap.put("345",new CountryCode("345","Mexico","MX","MEX"));
        countryCodeMap.put("347",new CountryCode("347","Martinique","MQ","MTQ"));
        countryCodeMap.put("348",new CountryCode("348","Montserrat","MS","MSR"));
        countryCodeMap.put("350",new CountryCode("350","Nicaragua","NI","NIC"));
        countryCodeMap.put("351",new CountryCode("351","Panama","PA","PAN"));
        countryCodeMap.put("352",new CountryCode("352","Panama","PA","PAN"));
        countryCodeMap.put("353",new CountryCode("353","Panama","PA","PAN"));
        countryCodeMap.put("354",new CountryCode("354","Panama","PA","PAN"));
        countryCodeMap.put("355",new CountryCode("355","Panama","PA","PAN"));
        countryCodeMap.put("356",new CountryCode("356","Panama","PA","PAN"));
        countryCodeMap.put("357",new CountryCode("357","Panama","PA","PAN"));
        countryCodeMap.put("358",new CountryCode("358","Puerto Rico","PR","PRI"));
        countryCodeMap.put("359",new CountryCode("359","El Salvador","SV","SLV"));
        countryCodeMap.put("361",new CountryCode("361","St Pierre Miquelon","PM","SPM"));
        countryCodeMap.put("362",new CountryCode("362","Trinidad Tobago","TT","TTO"));
        countryCodeMap.put("364",new CountryCode("364","Turks Caicos Is","TC","TCA"));
        countryCodeMap.put("366",new CountryCode("366","USA","US","USA"));
        countryCodeMap.put("367",new CountryCode("367","USA","US","USA"));
        countryCodeMap.put("368",new CountryCode("368","USA","US","USA"));
        countryCodeMap.put("369",new CountryCode("369","USA","US","USA"));
        countryCodeMap.put("370",new CountryCode("370","Panama","PA","PAN"));
        countryCodeMap.put("371",new CountryCode("371","Panama","PA","PAN"));
        countryCodeMap.put("372",new CountryCode("372","Panama","PA","PAN"));
        countryCodeMap.put("373",new CountryCode("373","Panama","PA","PAN"));
        countryCodeMap.put("374",new CountryCode("374","Panama","PA","PAN"));
        countryCodeMap.put("375",new CountryCode("375","St Vincent Grenadines","VC","VCT"));
        countryCodeMap.put("376",new CountryCode("376","St Vincent Grenadines","VC","VCT"));
        countryCodeMap.put("377",new CountryCode("377","St Vincent Grenadines","VC","VCT"));
        countryCodeMap.put("378",new CountryCode("378","British Virgin Is","VG","VGB"));
        countryCodeMap.put("379",new CountryCode("379","US Virgin Is","VI","VIR"));
        countryCodeMap.put("401",new CountryCode("401","Afghanistan","AF","AFG"));
        countryCodeMap.put("403",new CountryCode("403","Saudi Arabia","SA","SAU"));
        countryCodeMap.put("405",new CountryCode("405","Bangladesh","BD","BGD"));
        countryCodeMap.put("408",new CountryCode("408","Bahrain","BH","BHR"));
        countryCodeMap.put("410",new CountryCode("410","Bhutan","BT","BTN"));
        countryCodeMap.put("412",new CountryCode("412","China","CN","CHN"));
        countryCodeMap.put("413",new CountryCode("413","China","CN","CHN"));
        countryCodeMap.put("414",new CountryCode("414","China","CN","CHN"));
        countryCodeMap.put("416",new CountryCode("416","Taiwan","TW","TWN"));
        countryCodeMap.put("417",new CountryCode("417","Sri Lanka","LK","LKA"));
        countryCodeMap.put("419",new CountryCode("419","India","IN","IND"));
        countryCodeMap.put("422",new CountryCode("422","Iran","IR","IRN"));
        countryCodeMap.put("423",new CountryCode("423","Azerbaijan","AZ","AZE"));
        countryCodeMap.put("425",new CountryCode("425","Iraq","IQ","IRQ"));
        countryCodeMap.put("428",new CountryCode("428","Israel","IL","ISR"));
        countryCodeMap.put("431",new CountryCode("431","Japan","JP","JPN"));
        countryCodeMap.put("432",new CountryCode("432","Japan","JP","JPN"));
        countryCodeMap.put("434",new CountryCode("434","Turkmenistan","TM","TKM"));
        countryCodeMap.put("436",new CountryCode("436","Kazakhstan","KZ","KAZ"));
        countryCodeMap.put("437",new CountryCode("437","Uzbekistan","UZ","UZB"));
        countryCodeMap.put("438",new CountryCode("438","Jordan","JO","JOR"));
        countryCodeMap.put("440",new CountryCode("440","Korea","KR","KOR"));
        countryCodeMap.put("441",new CountryCode("441","Korea","KR","KOR"));
        countryCodeMap.put("443",new CountryCode("443","Palestine","PS","PSE"));
        countryCodeMap.put("445",new CountryCode("445","DPR Korea","KP","PRK"));
        countryCodeMap.put("447",new CountryCode("447","Kuwait","KW","KWT"));
        countryCodeMap.put("450",new CountryCode("450","Lebanon","LB","LBN"));
        countryCodeMap.put("451",new CountryCode("451","Kyrgyz Republic","ZZ","KGZ"));
        countryCodeMap.put("453",new CountryCode("453","Macao","ZZ","???"));
        countryCodeMap.put("455",new CountryCode("455","Maldives","MV","MDV"));
        countryCodeMap.put("457",new CountryCode("457","Mongolia","MN","MNG"));
        countryCodeMap.put("459",new CountryCode("459","Nepal","NP","NPL"));
        countryCodeMap.put("461",new CountryCode("461","Oman","OM","OMN"));
        countryCodeMap.put("463",new CountryCode("463","Pakistan","PK","PAK"));
        countryCodeMap.put("466",new CountryCode("466","Qatar","QA","QAT"));
        countryCodeMap.put("468",new CountryCode("468","Syria","SY","SYR"));
        countryCodeMap.put("470",new CountryCode("470","UAE","AE","ARE"));
        countryCodeMap.put("472",new CountryCode("472","Tajikistan","TJ","TJK"));
        countryCodeMap.put("473",new CountryCode("473","Yemen","YE","YEM"));
        countryCodeMap.put("475",new CountryCode("475","Yemen","YE","YEM"));
        countryCodeMap.put("477",new CountryCode("477","Hong Kong","HK","???"));
        countryCodeMap.put("478",new CountryCode("478","Bosnia and Herzegovina","BA","BIH"));
        countryCodeMap.put("501",new CountryCode("501","Antarctica","AQ","ATA"));
        countryCodeMap.put("503",new CountryCode("503","Australia","AU","AUS"));
        countryCodeMap.put("506",new CountryCode("506","Myanmar","MM","???"));
        countryCodeMap.put("508",new CountryCode("508","Brunei","BN","BRN"));
        countryCodeMap.put("510",new CountryCode("510","Micronesia","FM","FSM"));
        countryCodeMap.put("511",new CountryCode("511","Palau","PW","PLW"));
        countryCodeMap.put("512",new CountryCode("512","New Zealand","NZ","NZL"));
        countryCodeMap.put("514",new CountryCode("514","Cambodia","KH","KHM"));
        countryCodeMap.put("515",new CountryCode("515","Cambodia","KH","KHM"));
        countryCodeMap.put("516",new CountryCode("516","Christmas Is","CX","CXR"));
        countryCodeMap.put("518",new CountryCode("518","Cook Is","CK","COK"));
        countryCodeMap.put("520",new CountryCode("520","Fiji","FJ","FJI"));
        countryCodeMap.put("523",new CountryCode("523","Cocos Is","CC","CCK"));
        countryCodeMap.put("525",new CountryCode("525","Indonesia","ID","IDN"));
        countryCodeMap.put("529",new CountryCode("529","Kiribati","KI","KIR"));
        countryCodeMap.put("531",new CountryCode("531","Laos","LA","LAO"));
        countryCodeMap.put("533",new CountryCode("533","Malaysia","MY","MYS"));
        countryCodeMap.put("536",new CountryCode("536","N Mariana Is","MP","MNP"));
        countryCodeMap.put("538",new CountryCode("538","Marshall Is","MH","MHL"));
        countryCodeMap.put("540",new CountryCode("540","New Caledonia","NC","NCL"));
        countryCodeMap.put("542",new CountryCode("542","Niue","NU","NIU"));
        countryCodeMap.put("544",new CountryCode("544","Nauru","NR","NRU"));
        countryCodeMap.put("546",new CountryCode("546","French Polynesia","TF","PYF"));
        countryCodeMap.put("548",new CountryCode("548","Philippines","PH","PHL"));
        countryCodeMap.put("553",new CountryCode("553","Papua New Guinea","PG","PNG"));
        countryCodeMap.put("555",new CountryCode("555","Pitcairn Is","PN","PCN"));
        countryCodeMap.put("557",new CountryCode("557","Solomon Is","SB","SLB"));
        countryCodeMap.put("559",new CountryCode("559","American Samoa","AS","ASM"));
        countryCodeMap.put("561",new CountryCode("561","Samoa","WS","WSM"));
        countryCodeMap.put("563",new CountryCode("563","Singapore","SG","SGP"));
        countryCodeMap.put("564",new CountryCode("564","Singapore","SG","SGP"));
        countryCodeMap.put("565",new CountryCode("565","Singapore","SG","SGP"));
        countryCodeMap.put("566",new CountryCode("566","Singapore","SG","SGP"));
        countryCodeMap.put("567",new CountryCode("567","Thailand","TH","THA"));
        countryCodeMap.put("570",new CountryCode("570","Tonga","TO","TON"));
        countryCodeMap.put("572",new CountryCode("572","Tuvalu","TV","TUV"));
        countryCodeMap.put("574",new CountryCode("574","Vietnam","VN","VNM"));
        countryCodeMap.put("576",new CountryCode("576","Vanuatu","VU","VUT"));
        countryCodeMap.put("577",new CountryCode("577","Vanuatu","VU","VUT"));
        countryCodeMap.put("578",new CountryCode("578","Wallis Futuna Is","WF","WLF"));
        countryCodeMap.put("601",new CountryCode("601","South Africa","ZA","ZAF"));
        countryCodeMap.put("603",new CountryCode("603","Angola","AO","AGO"));
        countryCodeMap.put("605",new CountryCode("605","Algeria","DZ","DZA"));
        countryCodeMap.put("607",new CountryCode("607","St Paul Amsterdam Is","XX","???"));
        countryCodeMap.put("608",new CountryCode("608","Ascension Is","IO","???"));
        countryCodeMap.put("609",new CountryCode("609","Burundi","BI","BDI"));
        countryCodeMap.put("610",new CountryCode("610","Benin","BJ","BEN"));
        countryCodeMap.put("611",new CountryCode("611","Botswana","BW","BWA"));
        countryCodeMap.put("612",new CountryCode("612","Cen Afr Rep","CF","CAF"));
        countryCodeMap.put("613",new CountryCode("613","Cameroon","CM","CMR"));
        countryCodeMap.put("615",new CountryCode("615","Congo","CG","COG"));
        countryCodeMap.put("616",new CountryCode("616","Comoros","KM","COM"));
        countryCodeMap.put("617",new CountryCode("617","Cape Verde","CV","CPV"));
        countryCodeMap.put("618",new CountryCode("618","Antarctica","AQ","ATA"));
        countryCodeMap.put("619",new CountryCode("619","Ivory Coast","CI","???"));
        countryCodeMap.put("620",new CountryCode("620","Comoros","KM","COM"));
        countryCodeMap.put("621",new CountryCode("621","Djibouti","DJ","DJI"));
        countryCodeMap.put("622",new CountryCode("622","Egypt","EG","EGY"));
        countryCodeMap.put("624",new CountryCode("624","Ethiopia","ET","ETH"));
        countryCodeMap.put("625",new CountryCode("625","Eritrea","ER","ERI"));
        countryCodeMap.put("626",new CountryCode("626","Gabon","GA","GAB"));
        countryCodeMap.put("627",new CountryCode("627","Ghana","GH","GHA"));
        countryCodeMap.put("629",new CountryCode("629","Gambia","GM","GMB"));
        countryCodeMap.put("630",new CountryCode("630","Guinea-Bissau","GW","GNB"));
        countryCodeMap.put("631",new CountryCode("631","Equ. Guinea","GQ","GIN"));
        countryCodeMap.put("632",new CountryCode("632","Guinea","GN","GIN"));
        countryCodeMap.put("633",new CountryCode("633","Burkina Faso","BF","BFA"));
        countryCodeMap.put("634",new CountryCode("634","Kenya","KE","KEN"));
        countryCodeMap.put("635",new CountryCode("635","Antarctica","AQ","ATA"));
        countryCodeMap.put("636",new CountryCode("636","Liberia","LR","LBR"));
        countryCodeMap.put("637",new CountryCode("637","Liberia","LR","LBR"));
        countryCodeMap.put("642",new CountryCode("642","Libya","LY","LBY"));
        countryCodeMap.put("644",new CountryCode("644","Lesotho","LS","LSO"));
        countryCodeMap.put("645",new CountryCode("645","Mauritius","MU","MUS"));
        countryCodeMap.put("647",new CountryCode("647","Madagascar","MG","MDG"));
        countryCodeMap.put("649",new CountryCode("649","Mali","ML","MLI"));
        countryCodeMap.put("650",new CountryCode("650","Mozambique","MZ","MOZ"));
        countryCodeMap.put("654",new CountryCode("654","Mauritania","MR","MRT"));
        countryCodeMap.put("655",new CountryCode("655","Malawi","MW","MWI"));
        countryCodeMap.put("656",new CountryCode("656","Niger","NE","NER"));
        countryCodeMap.put("657",new CountryCode("657","Nigeria","NG","NGA"));
        countryCodeMap.put("659",new CountryCode("659","Namibia","NA","NAM"));
        countryCodeMap.put("660",new CountryCode("660","Reunion","RE","REU"));
        countryCodeMap.put("661",new CountryCode("661","Rwanda","RW","RWA"));
        countryCodeMap.put("662",new CountryCode("662","Sudan","SD","SDN"));
        countryCodeMap.put("663",new CountryCode("663","Senegal","SN","SEN"));
        countryCodeMap.put("664",new CountryCode("664","Seychelles","SC","SYC"));
        countryCodeMap.put("665",new CountryCode("665","St Helena","SH","SHN"));
        countryCodeMap.put("666",new CountryCode("666","Somalia","SO","SOM"));
        countryCodeMap.put("667",new CountryCode("667","Sierra Leone","SL","SLE"));
        countryCodeMap.put("668",new CountryCode("668","Sao Tome Principe","ST","STP"));
        countryCodeMap.put("669",new CountryCode("669","Swaziland","SZ","SWZ"));
        countryCodeMap.put("670",new CountryCode("670","Chad","TD","TCD"));
        countryCodeMap.put("671",new CountryCode("671","Togo","TG","TGO"));
        countryCodeMap.put("672",new CountryCode("672","Tunisia","TN","TUN"));
        countryCodeMap.put("674",new CountryCode("674","Tanzania","TZ","TZA"));
        countryCodeMap.put("675",new CountryCode("675","Uganda","UG","UGA"));
        countryCodeMap.put("676",new CountryCode("676","DR Congo","CD","COD"));
        countryCodeMap.put("677",new CountryCode("677","Tanzania","TZ","TZA"));
        countryCodeMap.put("678",new CountryCode("678","Zambia","ZM","ZMB"));
        countryCodeMap.put("679",new CountryCode("679","Zimbabwe","ZW","ZWE"));
        countryCodeMap.put("701",new CountryCode("701","Argentina","AR","ARG"));
        countryCodeMap.put("710",new CountryCode("710","Brazil","BR","BRA"));
        countryCodeMap.put("720",new CountryCode("720","Bolivia","BO","BOL"));
        countryCodeMap.put("725",new CountryCode("725","Chile","CL","CHL"));
        countryCodeMap.put("730",new CountryCode("730","Colombia","CO","COL"));
        countryCodeMap.put("735",new CountryCode("735","Ecuador","EC","ECU"));
        countryCodeMap.put("740",new CountryCode("740","UK","UK","GBR"));
        countryCodeMap.put("745",new CountryCode("745","Guiana","GF","GIN"));
        countryCodeMap.put("750",new CountryCode("750","Guyana","GY","GUY"));
        countryCodeMap.put("755",new CountryCode("755","Paraguay","PY","PRY"));
        countryCodeMap.put("760",new CountryCode("760","Peru","PE","PER"));
        countryCodeMap.put("765",new CountryCode("765","Suriname","SR","SUR"));
        countryCodeMap.put("770",new CountryCode("770","Uruguay","UY","URY"));
        countryCodeMap.put("775",new CountryCode("775","Venezuela","VE","VEN"));
    }


    public String getShiptypeForCode(Integer code){
        if(code == null) return "";
        if((code < 1) || (code > 99)){
            return "No such code : " + code;
        }
        return shipTypeMap.get(code);

    }

    public String getAnsi3ForCountryCode(String countryCode)  {

        CountryCode cc = countryCodeMap.get(countryCode);
        if(cc == null){
            LOG.error("Unknown country code: " + countryCode);
            return "ERR";
        }
        return cc.getAnsi3();
    }


    public String getBinaryForSymbol(char c)  {
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
        String ret = s.replace("@","").trim();
        return ret;
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