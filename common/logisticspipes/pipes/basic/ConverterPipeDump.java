package logisticspipes.pipes.basic;

import logisticspipes.LogisticsPipes;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ConverterPipeDump implements Opcodes {
	private static DynamicClassLoader	dynClassLoader;

	private static byte[] dump(String classId) {
		if(classId == null) return null;
		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;
		
		cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "logisticspipes/pipes/basic/ConverterPipe" + classId, null, "logisticspipes/pipes/basic/ConverterPipe", null);
		
		cw.visitSource("ConverterPipe" + classId + ".bytecode", null);
		
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(I)V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(5, l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitLdcInsn(classId);
			mv.visitMethodInsn(INVOKESPECIAL, "logisticspipes/pipes/basic/ConverterPipe", "<init>", "(ILjava/lang/String;)V");
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLineNumber(6, l1);
			mv.visitInsn(RETURN);
			Label l2 = new Label();
			mv.visitLabel(l2);
			mv.visitLocalVariable("this", "Llogisticspipes/pipes/basic/ConverterPipe" + classId + ";", null, l0, l2, 0);
			mv.visitLocalVariable("itemID", "I", null, l0, l2, 1);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}
		cw.visitEnd();
		return cw.toByteArray();
	}

	public static Class<?> getClassForId(int Id) {
		byte[] bytes = dump(Integer.toString(Id));
		if(dynClassLoader == null) dynClassLoader = new DynamicClassLoader(LogisticsPipes.class.getClassLoader());
		return dynClassLoader.defineClass("logisticspipes.pipes.basic.ConverterPipe" + Integer.toString(Id), bytes);
	}
	
	private static class DynamicClassLoader extends ClassLoader {
		public DynamicClassLoader(ClassLoader classLoader) {
			super(classLoader);
		}

		public Class<?> defineClass(String name, byte[] b) {
	        return defineClass(name, b, 0, b.length);
	    }
	}
}
