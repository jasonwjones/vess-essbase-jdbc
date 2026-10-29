package com.jasonwjones.hyperion.vess.outline;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads an outline from the XML Essbase exports it as, which carries the whole
 * outline in one document: every member with its aliases, formulas, UDAs and
 * attributes. It's read as a stream, so a large outline costs only the members
 * it holds.
 */
public final class OutlineParser {

	private OutlineParser() {
	}

	public static Outline parse(String application, String cube, byte[] xml) throws XMLStreamException {
		return parse(application, cube, new ByteArrayInputStream(xml));
	}

	public static Outline parse(String application, String cube, InputStream xml) throws XMLStreamException {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		XMLStreamReader reader = factory.createXMLStreamReader(xml);
		try {
			return read(new Outline(application, cube), reader);
		} finally {
			reader.close();
		}
	}

	private static Outline read(Outline outline, XMLStreamReader reader) throws XMLStreamException {
		Deque<Member> members = new ArrayDeque<>();
		Dimension dimension = null;
		// Where generation and level names are being read: Generations or Levels
		String names = null;
		// The element whose text is being collected, and the alias table for an Alias
		String collecting = null;
		String aliasTable = null;
		StringBuilder text = new StringBuilder();
		// MemberPropertiesInfo describes which properties are set, generation by
		// generation, which says nothing the members don't, so it's skipped
		int skipping = 0;

		while (reader.hasNext()) {
			int event = reader.next();
			if (event == XMLStreamConstants.START_ELEMENT) {
				String element = reader.getLocalName();
				if (skipping > 0 || element.equals("MemberPropertiesInfo")) {
					skipping++;
					continue;
				}
				switch (element) {
				case "applicationSettings":
					if (attribute(reader, "cubeType") != null) {
						outline.cubeType = attribute(reader, "cubeType");
					}
					break;
				case "bsoSettings":
					outline.dateFormat = attribute(reader, "dateFormat");
					break;
				case "Table":
					outline.aliasTables.add(attribute(reader, "name"));
					break;
				case "DefaultTable":
					outline.defaultAliasTable = attribute(reader, "name");
					break;
				case "Dimension":
					dimension = new Dimension(attribute(reader, "name"), outline.dimensions.size() + 1);
					dimension.type = dimensionType(attribute(reader, "dimensionType"));
					if ("Dense".equalsIgnoreCase(attribute(reader, "density"))) {
						dimension.density = "Dense";
					}
					dimension.attributeType = attribute(reader, "AttributeType");
					outline.dimensions.add(dimension);
					Member root = new Member(dimension.getName(), dimension, null);
					properties(root, reader);
					dimension.root = root;
					dimension.members.add(root);
					members.push(root);
					break;
				case "Member":
					if (dimension != null && !members.isEmpty()) {
						Member member = new Member(attribute(reader, "name"), dimension, members.peek());
						properties(member, reader);
						dimension.members.add(member);
						members.push(member);
					}
					break;
				case "AttributeDimension":
					if (dimension != null && members.size() == 1) {
						dimension.attributeDimensions.add(attribute(reader, "name"));
					}
					break;
				case "AttributeMember":
					if (!members.isEmpty()) {
						members.peek().attributes.put(attribute(reader, "dimension"), attribute(reader, "name"));
					}
					break;
				case "Generations":
				case "Levels":
					names = element;
					break;
				case "GenLevel":
					if (dimension != null && names != null) {
						String name = attribute(reader, "name");
						Integer number = integer(attribute(reader, "number"));
						if (name != null && number != null) {
							(names.equals("Levels") ? dimension.levelNames : dimension.generationNames).put(number, name);
						}
					}
					break;
				case "Alias":
					aliasTable = attribute(reader, "table");
					collecting = element;
					text.setLength(0);
					break;
				case "UDA":
				case "MemberFormula":
				case "Comment":
					collecting = element;
					text.setLength(0);
					break;
				default:
					break;
				}
			} else if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
				if (collecting != null && skipping == 0) {
					text.append(reader.getText());
				}
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				String element = reader.getLocalName();
				if (skipping > 0) {
					skipping--;
					continue;
				}
				switch (element) {
				case "Dimension":
					members.clear();
					dimension = null;
					break;
				case "Member":
					if (!members.isEmpty()) {
						members.pop();
					}
					break;
				case "Generations":
				case "Levels":
					names = null;
					break;
				case "Alias":
					if (!members.isEmpty() && aliasTable != null) {
						members.peek().aliases.put(aliasTable, text.toString().trim());
					}
					collecting = null;
					break;
				case "UDA":
					if (!members.isEmpty()) {
						members.peek().udas.add(text.toString().trim());
					}
					collecting = null;
					break;
				case "MemberFormula":
					if (!members.isEmpty()) {
						members.peek().formula = text.toString().strip();
					}
					collecting = null;
					break;
				case "Comment":
					if (!members.isEmpty()) {
						members.peek().comment = text.toString().strip();
					}
					collecting = null;
					break;
				default:
					break;
				}
			}
		}
		if (outline.aliasTables.isEmpty()) {
			outline.aliasTables.add(outline.defaultAliasTable);
		}
		outline.complete();
		return outline;
	}

	private static void properties(Member member, XMLStreamReader reader) {
		member.storage = Member.Storage.parse(attribute(reader, "DataStorage"));
		String consolidation = attribute(reader, "Consolidation");
		if (consolidation != null && consolidation.length() == 1) {
			member.consolidation = consolidation;
		}
		member.timeBalance = flag(attribute(reader, "TimeBalance"));
		member.varianceReporting = flag(attribute(reader, "VarianceReporting"));
		member.twoPass = "Y".equals(attribute(reader, "TwoPassCalc"));
		if ("Y".equals(attribute(reader, "SharedMember")) || member.storage.equals(Member.Storage.SHARED)) {
			member.shared = true;
			member.storage = Member.Storage.SHARED;
			member.baseMember = baseName(attribute(reader, "BaseMemberName"), member.getName());
		}
	}

	/**
	 * A setting that the XML gives as N when it's off, and otherwise names.
	 */
	private static String flag(String value) {
		return value == null || value.equals("N") ? null : value;
	}

	/**
	 * The base member's own name, from its qualified one: 100-20 from
	 * [100].[100-20].
	 */
	static String baseName(String qualified, String fallback) {
		if (qualified == null || qualified.isEmpty()) {
			return fallback;
		}
		int open = qualified.lastIndexOf("[");
		if (open >= 0 && qualified.endsWith("]")) {
			return qualified.substring(open + 1, qualified.length() - 1).replace("]]", "]");
		}
		return qualified;
	}

	private static String dimensionType(String xml) {
		if (xml == null) {
			return Dimension.REGULAR;
		}
		switch (xml) {
		case "Time":
			return Dimension.TIME;
		case "Accounts":
			return Dimension.ACCOUNTS;
		case "Attribute":
			return Dimension.ATTRIBUTE;
		case "CurrencyPartition":
			return "Currency Partition";
		default:
			return xml;
		}
	}

	private static String attribute(XMLStreamReader reader, String name) {
		for (int index = 0; index < reader.getAttributeCount(); index++) {
			if (reader.getAttributeLocalName(index).equals(name)) {
				return reader.getAttributeValue(index);
			}
		}
		return null;
	}

	private static Integer integer(String text) {
		try {
			return text == null ? null : Integer.valueOf(text.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
