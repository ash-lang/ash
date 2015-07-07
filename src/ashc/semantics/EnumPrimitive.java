package ashc.semantics;

import ashc.semantics.Semantics.TypeI;

/**
 * Ash
 *
 * @author samtebbs, 21:02:48 - 24 May 2015
 */
public enum EnumPrimitive {

    BOOL("bool", "boolean", "Z", false, false),
    DOUBLE("double", "double", "D", true, false),
    FLOAT("float", "float", "F", true, false),
    LONG("long", "long", "J", true, false),
    INT("int", "int", "I", true, true),
    SHORT("short", "short", "S", true, true),
    BYTE("byte", "byte", "B", true, true),
    UBYTE("ubyte", "byte", "B", true, false),
    USHORT("ushort", "short", "S", true, false),
    ULONG("ulong", "long", "J", true, false),
    UINT("uint", "int", "I", true, false),
    CHAR("char", "char", "C", false, false);

    public String ashName, javaName, bytecodeName;
    public boolean validForArrayIndex, isNumeric;

    private EnumPrimitive(final String ashName, final String javaName, String bytecodeName, final boolean isNumeric, final boolean wholeNumber) {
	this.ashName = ashName;
	this.javaName = javaName;
	this.bytecodeName = bytecodeName;
	this.isNumeric = isNumeric;
	validForArrayIndex = wholeNumber;
    }

    public static boolean isPrimitive(final String typeName) {
	for (final EnumPrimitive p : EnumPrimitive.values())
	    if (p.ashName.equals(typeName)) return true;
	return false;
    }

    public static EnumPrimitive getPrimitive(final String name) {
	for (final EnumPrimitive p : EnumPrimitive.values())
	    if (p.ashName.equals(name)) return p;
	return null;
    }

    public static boolean validForArrayIndex(final TypeI indexType) {
	return isPrimitive(indexType.shortName) && (indexType.arrDims == 0) && getPrimitive(indexType.shortName).validForArrayIndex;
    }

    public static boolean isNumeric(final String shortName) {
	final EnumPrimitive p = getPrimitive(shortName);
	if (p != null) return p.isNumeric;
	return false;
    }

    public static boolean isJavaPrimitive(final String typeName) {
	for (final EnumPrimitive p : EnumPrimitive.values())
	    if (p.javaName.equals(typeName)) return true;
	return false;
    }

}
