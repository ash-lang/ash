package ashc.semantics;

import java.util.*;

import org.objectweb.asm.*;

import ashc.codegen.*;
import ashc.codegen.GenNode.EnumInstructionOperand;
import ashc.codegen.GenNode.GenNodeField;
import ashc.codegen.GenNode.GenNodeFieldStore;
import ashc.codegen.GenNode.GenNodeFuncCall;
import ashc.codegen.GenNode.GenNodeFunction;
import ashc.codegen.GenNode.GenNodeReturn;
import ashc.codegen.GenNode.GenNodeThis;
import ashc.codegen.GenNode.GenNodeType;
import ashc.codegen.GenNode.GenNodeVar;
import ashc.codegen.GenNode.GenNodeVarLoad;
import ashc.grammar.Node.IExpression;
import ashc.grammar.Node.NodeBool;
import ashc.grammar.Node.NodeDouble;
import ashc.grammar.Node.NodeFloat;
import ashc.grammar.Node.NodeInteger;
import ashc.grammar.Node.NodeLong;
import ashc.grammar.Node.NodeNull;
import ashc.grammar.Node.NodeTupleType;
import ashc.grammar.Node.NodeType;

public class TypeI {

    private static TypeI objectType = new TypeI("Object", 0, false), voidType = new TypeI("void", 0, false), stringType = new TypeI("String", 0, false);
    public String shortName, tupleName;
    public int arrDims;
    public boolean optional;
    public LinkedList<TypeI> tupleTypes, genericTypes;
    public QualifiedName qualifiedName;

    public TypeI(final String shortName, final int arrDims, final boolean optional) {
	this.shortName = shortName;
	this.arrDims = arrDims;
	this.optional = optional;
	tupleTypes = new LinkedList<TypeI>();
	genericTypes = new LinkedList<TypeI>();
    }

    public TypeI(final NodeType type) {
	this(type.id, type.arrDims, type.optional);
	for (final NodeType nodeType : type.generics.types)
	    genericTypes.add(new TypeI(nodeType));
	for (final NodeType nodeType : type.tupleTypes)
	    tupleTypes.add(new TypeI(nodeType));
    }

    public TypeI(final EnumPrimitive primitive) {
	this(primitive.ashName, 0, false);
    }

    public TypeI(final EnumPrimitive primitive, final int arrDims) {
	this(primitive.ashName, arrDims, false);
    }

    public TypeI(final NodeTupleType nodeType) {
	this(nodeType.type);
	tupleName = nodeType.name;

	for (final NodeType t : nodeType.type.generics.types)
	    genericTypes.add(new TypeI(t));
    }

    public static TypeI fromClass(final Class cls) {
	String clsName = cls.getName();
	if (EnumPrimitive.isJavaPrimitive(clsName)) return new TypeI(EnumPrimitive.getFromJavaPrimitive(clsName));
	int arrDims = clsName.length();
	clsName = clsName.replace("[", "");
	arrDims = arrDims - clsName.length();
	final String shortName = clsName.substring(clsName.lastIndexOf('.') + 1);
	if (clsName.charAt(0) == 'L') clsName = clsName.substring(1);
	clsName = clsName.replace(";", "");
	final TypeI type = new TypeI(shortName, arrDims, false);
	type.qualifiedName = new QualifiedName("");
	for (final String section : clsName.split("\\."))
	    type.qualifiedName.add(section);
	/*
	 * if(!clsName.equals("void") && !EnumPrimitive.isJavaPrimitive(clsName)){ boolean isGeneric = false; for(TypeVariable tVar : cls.getTypeParameters())
	 * if(tVar.getName().equals(clsName)){ isGeneric = true; break; } if(!isGeneric) TypeImporter.loadClass(clsName); }
	 */
	// Since all Java types are nullable, this must be set to optional
	type.optional = true;
	return type;
    }

    @Override
    public boolean equals(final Object obj) {
	if (obj instanceof TypeI) {
	    final TypeI type = (TypeI) obj;
	    return type.shortName.equals(shortName) && (type.arrDims == arrDims) && (type.optional == optional);
	}
	return false;
    }

    @Override
    public String toString() {
	if (isNull() || isVoid()) return shortName;
	final StringBuffer arrBuffer = new StringBuffer();
	for (int i = 0; i < arrDims; i++)
	    arrBuffer.append("[]");
	String id = shortName;
	if (tupleName != null) id = tupleName + " : " + id;
	if (isTuple()) {
	    id = "(";
	    for (int i = 0; i < tupleTypes.size(); i++)
		id += tupleTypes.get(i).toString() + (i < (tupleTypes.size() - 1) ? ", " : "");
	    id += ")";
	}
	if (genericTypes.size() > 0) {
	    id += "<";
	    for (int i = 0; i < (genericTypes.size() - 1); i++)
		id += genericTypes.toString() + ", ";
	    id += genericTypes.getLast().toString() + ">";
	}
	return String.format("%s%s%s", id, arrBuffer.toString(), optional ? "?" : "");
    }

    public boolean isVoid() {
	return !isTuple() ? shortName.equals("void") : false;
    }

    public boolean canBeAssignedTo(final TypeI exprType) {
	if (exprType == null) return false;
	if (equals(exprType)) return true;
	// If the expr is null, and this is optional, and it has more than 0
	// array dimensions
	if (exprType.isNull() && optional && (!EnumPrimitive.isPrimitive(shortName) || (arrDims > 0))) return true;
	// If this is a tuple and the expression is a tuple expression
	if (tupleTypes.size() > 0) {
	    if (tupleTypes.size() == exprType.tupleTypes.size()) {
		for (int i = 0; i < exprType.tupleTypes.size(); i++) {
		    final TypeI tupleType1 = tupleTypes.get(i), tupleType2 = exprType.tupleTypes.get(i);
		    if (!tupleType1.tupleName.equals(tupleType2.tupleName) || !tupleType1.canBeAssignedTo(tupleType2)) return false;
		}
		return true;
	    }
	    return false;
	}

	// Optionals can be assigned to non-optionals, but not the other way
	// around
	if (!optional && exprType.optional) return false;
	// If they are both numeric and the array dimensions are 0
	if (EnumPrimitive.isNumeric(shortName) && EnumPrimitive.isNumeric(exprType.shortName) && (arrDims == exprType.arrDims)) return true;

	return (exprType.arrDims == arrDims)
		&& (shortName.equals(exprType.shortName) || (exprType.isNull() && !EnumPrimitive.isNumeric(shortName)) || Semantics.typeHasSuper(exprType.shortName, shortName));
    }

    public boolean isNull() {
	return !isTuple() ? shortName.equals("null") : false;
    }

    public boolean isTuple() {
	return (tupleTypes != null) && (tupleTypes.size() > 0);
    }

    public static TypeI getVoidType() {
	return voidType;
    }

    public boolean isArray() {
	return arrDims > 0;
    }

    public TypeI copy() {
	return new TypeI(shortName, arrDims, optional);
    }

    public static TypeI getObjectType() {
	return objectType;
    }

    public TypeI setArrDims(final int i) {
	arrDims = i;
	return this;
    }

    public TypeI setOptional(final boolean b) {
	optional = b;
	return this;
    }

    public boolean isNumeric() {
	return EnumPrimitive.isNumeric(shortName) && (arrDims == 0);
    }

    public static TypeI getStringType() {
	return stringType;
    }

    public boolean isPrimitive() {
	return EnumPrimitive.isPrimitive(shortName) && !isArray();
    }

    public boolean isRange() {
	return (shortName != null) & shortName.equals("Range");
    }

    public String toBytecodeName() {
	StringBuffer name = new StringBuffer();
	if (isArray()) for (int i = 0; i < arrDims; i++)
	    name.append("[");
	if (isTuple()) {
	    // The tuple class name is "Tuple" followed by the number of
	    // fields
	    final String tupleClassName = "Tuple" + tupleTypes.size();
	    if (!GenNode.generatedTupleClasses.contains(tupleClassName)) {
		final GenNodeType tupleClass = new GenNodeType(tupleClassName, tupleClassName, "Ljava/lang/Object;", null, Opcodes.ACC_PUBLIC);
		GenNode.addGenNodeType(tupleClass);
		final GenNodeFunction tupleConstructor = new GenNodeFunction("<init>", Opcodes.ACC_PUBLIC, "V");
		GenNode.addGenNodeFunction(tupleConstructor);
		int tupleTypeNum = 1;
		char tupleFieldName = 'a', tupleFieldType = 'A';
		tupleConstructor.stmts.add(new GenNodeThis());
		tupleConstructor.stmts.add(new GenNodeFuncCall("java/lang/Object", "<init>", "()V", false, false, false, true));
		for (final TypeI tupleType : tupleTypes) {
		    final String tupleFieldNameStr = String.valueOf(tupleFieldName), tupleFieldTypeStr = String.valueOf(tupleFieldType);
		    tupleClass.generics.add(tupleFieldTypeStr);
		    tupleClass.addField(new GenNodeField(Opcodes.ACC_PUBLIC, tupleFieldNameStr, "Ljava/lang/Object;", tupleFieldTypeStr));
		    tupleConstructor.params.add(TypeI.getObjectType());
		    tupleConstructor.stmts.add(new GenNodeThis());
		    tupleConstructor.stmts.add(new GenNodeVar(tupleFieldName + "Arg", "Ljava/lang/Object;", tupleTypeNum, "T" + tupleType.toBytecodeName()
			    + ";"));
		    tupleConstructor.stmts.add(new GenNodeVarLoad(EnumInstructionOperand.REFERENCE, tupleTypeNum));
		    tupleConstructor.stmts.add(new GenNodeFieldStore(tupleFieldNameStr, tupleClassName, "Ljava/lang/Object;", false));
		    tupleTypeNum++;
		    tupleFieldName++;
		    tupleFieldType++;
		}
		tupleConstructor.stmts.add(new GenNodeReturn());
		GenNode.exitGenNodeFunction();
		GenNode.exitGenNodeType();
		GenNode.generatedTupleClasses.add(tupleClassName);
	    }
	    name = new StringBuffer("L" + tupleClassName + ";");
	} else if (isVoid()) name.append("V");
	else if (EnumPrimitive.isPrimitive(shortName)) name.append(EnumPrimitive.getPrimitive(shortName).bytecodeName);
	else name.append("L" + Semantics.getType(shortName).get().qualifiedName.toString().replace('.', '/') + ";");
	return name.toString();
    }

    public EnumInstructionOperand getInstructionType() {
	if (isArray()) return EnumInstructionOperand.ARRAY;
	else if (isPrimitive()) return EnumPrimitive.getPrimitive(shortName).instructionType;
	else return EnumInstructionOperand.REFERENCE;
    }

    public IExpression getDefaultValue() {
	if (isPrimitive()) switch (EnumPrimitive.getPrimitive(shortName)) {
	    case BOOL:
		return new NodeBool(0, 0, false);
	    case LONG:
		return new NodeLong(0, 0, 0);
	    case DOUBLE:
		return new NodeDouble(0, 0, 0d);
	    case FLOAT:
		return new NodeFloat(0, 0, 0f);
	    default:
		return new NodeInteger(0, 0, 0);
	}
	else return new NodeNull();
    }

    public boolean isValidArrayAccessor() {
	return isNumeric() && EnumPrimitive.getPrimitive(shortName).validForArrayIndex;
    }

    public TypeI setTupleName(final String valueOf) {
	tupleName = valueOf;
	return this;
    }
}