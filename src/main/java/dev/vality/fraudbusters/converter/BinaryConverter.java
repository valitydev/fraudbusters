package dev.vality.fraudbusters.converter;

public interface BinaryConverter<T> {

    T convert(byte[] bin, Class<T> clazz);

}
