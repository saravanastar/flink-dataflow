package com.dataartisans.flink.dataflow.translation;

import org.apache.flink.api.java.ExecutionEnvironment;

import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.Pipeline.PipelineVisitor;
import com.google.cloud.dataflow.sdk.runners.TransformTreeNode;
import com.google.cloud.dataflow.sdk.transforms.PTransform;
import com.google.cloud.dataflow.sdk.values.PValue;


public class FlinkTranslator implements PipelineVisitor {
	
	private final TranslationContext context;
	
	
	public FlinkTranslator(ExecutionEnvironment env) {
		this.context = new TranslationContext(env);
	}

	
	public void translate(Pipeline pipeline) {
		pipeline.traverseTopologically(this);
	}
	

	// --------------------------------------------------------------------------------------------
	//  Pipeline Visitor Methods
	// --------------------------------------------------------------------------------------------
	
	private String genSpaces(int n) {
		String s = "";
		for(int i = 0; i < n; i++) {
			s += "|   ";
		}
		return s;
	}
	
	@Override
	public void enterCompositeTransform(TransformTreeNode node) {
		System.out.println(genSpaces(this.depth) + "enterCompositeTransform- " + node.toString().split("@")[1]);
		this.currentCompositeTransformNode = node;
		this.depth++;
	}

	@Override
	public void leaveCompositeTransform(TransformTreeNode node) {
		this.depth--;
		System.out.println(genSpaces(this.depth) + "leaveCompositeTransform- " + node.toString().split("@")[1]);
		this.currentCompositeTransformNode = null;
	}

	@Override
	public void visitTransform(TransformTreeNode node) {
		System.out.println(genSpaces(this.depth) + "visitTransform- " + node.toString().split("@")[1]);
		
		// the transformation applied in this node
		PTransform<?, ?> transform = node.getTransform();
		
		// the translator to the Flink operation(s)
		TransformToFlinkOpTranslator<?> translator = FlinkTransformTranslators.getTranslator(transform);
		
		if (translator == null) {
			throw new UnsupportedOperationException("The transform " + transform + " is currently not supported.");
		}
		
		applyTransform(transform, node, translator);

	}

	@Override
	public void visitValue(PValue value, TransformTreeNode producer) {
		// do nothing here
		System.out.println(genSpaces(this.depth) + "  ^-visitValue- value=" + value + " producer=" + producer);
	}
	
	/**
	 * Utility method to define a generic variable to cast the translator and the transform to.
	 * 
	 * @param transform
	 * @param node
	 * @param translator
	 */
	private <T extends PTransform<?, ?>> void applyTransform(PTransform<?, ?> transform, TransformTreeNode node, TransformToFlinkOpTranslator<?> translator) {
		
		@SuppressWarnings("unchecked")
		T typedTransform = (T) transform;
		
		@SuppressWarnings("unchecked")
		TransformToFlinkOpTranslator<T> typedTranslator = (TransformToFlinkOpTranslator<T>) translator;
		
		typedTranslator.translateNode(node, typedTransform, context);
	}
}
