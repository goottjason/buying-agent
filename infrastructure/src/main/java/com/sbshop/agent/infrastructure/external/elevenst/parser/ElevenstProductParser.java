package com.sbshop.agent.infrastructure.external.elevenst.parser;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Component
public class ElevenstProductParser {

  /**
   * XML 문자열을 자바 DOM 객체(Document)로 변환합니다.
   */
  public Document parseXml(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(xml)));
  }

  /**
   * 특정 태그(<TagName>) 안의 텍스트를 안전하게 가져옵니다.
   * (CDATA 블록으로 감싸져 있어도 알아서 알맹이만 빼줍니다!)
   */
  public String getText(Document doc, String tagName) {
    NodeList nodes = doc.getElementsByTagName(tagName);
    if (nodes != null && nodes.getLength() > 0) {
      return nodes.item(0).getTextContent().trim();
    }
    return ""; // 태그가 없거나 비어있으면 빈 문자열 반환
  }
}