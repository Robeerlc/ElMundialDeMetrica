package com.metrica.porramundial.dto;

import com.metrica.porramundial.domain.Country;
import com.metrica.porramundial.domain.Department;

public record RegisterRequest(String email, String fullName, String password, Country country, Department department) {}