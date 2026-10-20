package com.liugh.service.entity;

public interface DTOConvert <S,T>{
	T convertFor(S s);
}
