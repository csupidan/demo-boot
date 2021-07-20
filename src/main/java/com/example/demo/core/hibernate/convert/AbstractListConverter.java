package com.example.demo.core.hibernate.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.AttributeConverter;

public abstract class AbstractListConverter<T> extends AbstractCollectionConverter<T>
		implements AttributeConverter<List<T>, String> {

	@Override
	public String convertToDatabaseColumn(List<T> list) {
		return doConvertToDatabaseColumn(list);
	}

	@Override
	public List<T> convertToEntityAttribute(String string) {
		return (List<T>) super.doConvertToEntityAttribute(string);
	}

	@Override
	protected Collection<T> collection() {
		return new ArrayList<>();
	}

	@Override
	protected abstract T convert(String s);

}