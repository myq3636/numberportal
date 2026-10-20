package com.liugh.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExcelUtil {
	private static List<Object[]> mapDataset(ArrayList<LinkedHashMap<String , Object>> allList, int len) {
		List<Object[]> dataset = new ArrayList<Object[]>();
		if (allList == null) {
			String str[] = new String[len];
			dataset.add(str);
		} else {
			for (LinkedHashMap<String, Object> map : allList) {// 拿到第i个数据
                //对第i个数组进行迭?
				Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
				int i = 0;
				Object[] str = new Object[map.size()];// 保存数据的字符串数组
				while (iter.hasNext()) {
					Object value = iter.next().getValue();
					if (value == null) {
						str[i] = "";
					} else {
						str[i] = value;
					}
					i++;
				}
				dataset.add(str);
			}
		}		
		return dataset;
	}
	
	public static void exportExcel(List<String> title, List<String[]> headers,
			List<Object> dataList, OutputStream out) {
		SXSSFWorkbook  workbook = new SXSSFWorkbook ();
	     int  sheetlen=dataList.size();
	    for (int i = 0; i<sheetlen; i++) {
				// excel中生成一个表
	    	     String titles=title.get(i);
			     Sheet sheet =workbook.createSheet(titles);
			    //给表格加上边框，并且内容居中			     
			    CellStyle  cellStyle= workbook.createCellStyle();
		        cellStyle.setAlignment(HorizontalAlignment.CENTER);//居中
		        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);//垂直
		        cellStyle.setBorderBottom(BorderStyle.THIN);//下边框    
		        cellStyle.setBorderLeft(BorderStyle.THIN);//左边框    
		        cellStyle.setBorderTop(BorderStyle.THIN);//上边框    
		        cellStyle.setBorderRight(BorderStyle.THIN);//右边框    		      
		        DataFormat format = workbook.createDataFormat();
		        cellStyle.setDataFormat(format.getFormat("@"));
		        Font titleFont = workbook.createFont();
				titleFont.setFontHeightInPoints((short)10);
				cellStyle.setFont(titleFont);
				String header[]=headers.get(i);
		        int len =header.length-1;
		     // 设置第一行跨的行数，列数
				sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, len));
				Row row0 = sheet.createRow(0);
				Cell ce = row0.createCell(0);
		        ce.setCellValue(titles);
				ce.setCellStyle(cellStyle); // 样式，居�?
				//生成表头信息
				Row row = sheet.createRow(1);
				int width1 = 0;
					for (int k = 0; k <=len; k++){
						Cell cell1 = row.createCell(k);
						width1 = header[k].getBytes().length;
						sheet.setColumnWidth(k,(width1*256*2));
						XSSFRichTextString text = new XSSFRichTextString(header[k]);
						cell1.setCellValue(text);
						cell1.setCellStyle(cellStyle);
						cell1.setCellType(Cell.CELL_TYPE_STRING);
					}
					/**
					 * 生成表格信息
					 */
						ArrayList<LinkedHashMap<String , Object>> listdata=(ArrayList<LinkedHashMap<String, Object>>) dataList.get(i);
						List<Object[]> dataset = mapDataset(listdata, header.length);
						Iterator<Object[]> it = dataset.iterator();
						int index = 1;
						while (it.hasNext()) {
							index++;
							row = sheet.createRow(index);
							Object[] t = (Object[]) it.next();
							for (int s = 0; s < t.length; s++) {
								Cell cell = row.createCell(s);
								// sheet.autoSizeColumn((short)i,true);
								// 判断值的类型后进行强制类型转�?
								if (t[s] instanceof Integer) {
									cell.setCellStyle(cellStyle);
									cell.setCellValue((Integer) t[s]);
								} else if (t[s] instanceof Double) {
									cell.setCellStyle(cellStyle);
									cell.setCellValue(Double.parseDouble(t[s] + ""));
								} else if (t[s] instanceof Float) {
									cell.setCellStyle(cellStyle);
									cell.setCellValue(Float.parseFloat(t[s] + ""));
								} else {
									String data = "";
									if (t[s] != null && !"null".equals(t[s])
											&& !"".equals(t[s])) {
										data = t[s] + "";
									}
									/*if (isValidInt(data)) {
										cell.setCellStyle(cellStyle);
										cell.setCellValue(Integer.parseInt(data));
									} */
									String textValue = data == null ? "" : (data + "");
									XSSFRichTextString richString = new XSSFRichTextString(textValue);
									cell.setCellValue(richString);
									cell.setCellStyle(cellStyle);
								}
		
							}
						}  		
	      }		
	    try {
			workbook.write(out);
		} catch (IOException e) {
			System.out.println(e);
		}finally {
			try {
				workbook.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println(e);
			}
		}		
	    
	}
	public static synchronized String mapOutToExcel(String filename, List<String> title,List<String[]> headers,
			List<Object> allList) {
		FileOutputStream  out = null;
		try {	
			out = new FileOutputStream(filename);
			exportExcel(title, headers, allList, out);
			out.flush();
		} catch (IOException e) {
			System.out.println("OutToExcel OutputStream " + e.getMessage());
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				System.out.println("OutToExcel OutputStream close " + e.getMessage());
			}
		}
		return null;
	}
	
	
	public static void main(String[] args) {    	
    	String filenames="C:\\king\\test\\工单信息表.xlsx";
    	System.out.println("start to execut export excel------"+System.currentTimeMillis());    	
    	List<String[]> header=new ArrayList<String[]>();    	
    	List<Object> listall=new ArrayList<Object>();    	
    	
    	List<String> title=new ArrayList<String>();
    	for(int j=0;j<=5;j++) {
    		List<LinkedHashMap<String,Object>> list1=new ArrayList<LinkedHashMap<String,Object>>();    		
    		String title1="你好啊"+j;
    		title.add(title1);
    		String []head={"姓名","年龄","性别"};    	
        	header.add(head);
    		for(int i=0;i<=500;i++){
    	    	LinkedHashMap<String,Object> hashmap=new LinkedHashMap<String,Object>();	
    	    	hashmap.put("name", "张三");	
    	    	hashmap.put("age", "25");
    	    	hashmap.put("sex", "男");
    	    	list1.add(hashmap);
        	}
    		listall.add(list1);
    	}    	    	
    	mapOutToExcel(filenames, title, header, listall); 
    	System.out.println("end to execut export excel------"+System.currentTimeMillis());
  }

	
}
