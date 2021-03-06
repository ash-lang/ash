package ash.semantics;

import ash.semantics.member.Type;

/**
 * Created by samtebbs on 19/03/2016.
 */
public class TypeInstance {

    public static final TypeInstance BOOL = new TypeInstance(new QualifiedName("bool")),
            CHAR = new TypeInstance(new QualifiedName("char")),
            DOUBLE = new TypeInstance(new QualifiedName("double")),
            FLOAT = new TypeInstance(new QualifiedName("float")),
            INT = new TypeInstance(new QualifiedName("int")),
            STRING = new TypeInstance(new QualifiedName("java.lang.String"));
    public final int arrDims;
    public final QualifiedName qualifiedName;

    public TypeInstance(int arrDims, QualifiedName qualifiedName) {
        this.arrDims = arrDims;
        this.qualifiedName = qualifiedName;
    }

    public TypeInstance(QualifiedName qualifiedName) {
        this(0, qualifiedName);
    }

    public boolean canBeAssignedTo(TypeInstance exprType) {
        // TODO
        return false;
    }

    @Override
    public String toString() {
        return qualifiedName.toString();
    }

    public static TypeInstance fromType(Type type) {
        return new TypeInstance(type.name);
    }

    public static TypeInstance fromASMType(jdk.internal.org.objectweb.asm.Type type) {
        int arrayDims = 0;
        if(type.getSort() == jdk.internal.org.objectweb.asm.Type.ARRAY) {
            arrayDims = type.getDimensions();
            type = type.getElementType();
        }
        return new TypeInstance(arrayDims, new QualifiedName(type.getClassName()));
    }
}
