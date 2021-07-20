package com.example.demo.core.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Getter;
import lombok.Setter;

public class JsonSerializerTests {

	ObjectMapper mapper = new ObjectMapper();

	@Getter
	@Setter
	static class User {

		private String username;

		@JsonSerialize(using = ToIdSerializer.class)
		@JsonDeserialize(using = FromIdDeserializer.class)
		private Department department;

		@JsonSerialize(using = ToIdSerializer.class)
		@JsonDeserialize(using = FromIdDeserializer.class)
		private List<Department> departments = new ArrayList<>();

		@JsonSerialize(using = ToIdSerializer.class)
		@JsonDeserialize(using = FromIdDeserializer.class)
		private Department[] depts;

	}

	@Getter
	@Setter
	static class Department {

		private Long id;

		private String name;

	}

	@Test
	public void testToIdSerializer() throws IOException {
		User u = new User();
		u.setUsername("username");
		Department department = new Department();
		department.setId(12L);
		department.setName("department");
		Department department2 = new Department();
		department2.setId(13L);
		department2.setName("department2");
		u.setDepartment(department);
		u.getDepartments().add(department);
		u.getDepartments().add(department2);
		u.setDepts(u.getDepartments().toArray(new Department[u.getDepartments().size()]));
		String json = mapper.writeValueAsString(u);
		JsonNode root = mapper.readTree(json);
		JsonNode node = root.get("department");
		assertThat(node.isNumber()).isTrue();
		assertThat(node.asLong()).isEqualTo(department.getId());
		node = root.get("departments");
		assertThat(node.isArray()).isTrue();
		assertThat(node.get(0).asLong()).isEqualTo(department.getId());
		assertThat(node.get(1).asLong()).isEqualTo(department2.getId());
		node = root.get("depts");
		assertThat(node.isArray()).isTrue();
		assertThat(node.get(0).asLong()).isEqualTo(department.getId());
		assertThat(node.get(1).asLong()).isEqualTo(department2.getId());
		testFromIdSerializer(json, department, department2);
		json = "{\"username\":\"username\",\"department\":{\"id\":12},\"departments\":[{\"id\":12},{\"id\":13}],\"depts\":[{\"id\":12},{\"id\":13}]}";
		testFromIdSerializer(json, department, department2);
	}

	private void testFromIdSerializer(String json, Department department, Department department2) throws IOException {
		User user = mapper.readValue(json, User.class);
		assertThat(user.getDepartment().getId()).isEqualTo(department.getId());
		assertThat(user.getDepartments().get(0).getId()).isEqualTo(department.getId());
		assertThat(user.getDepartments().get(1).getId()).isEqualTo(department2.getId());
		assertThat(user.getDepts()[0].getId()).isEqualTo(department.getId());
		assertThat(user.getDepts()[1].getId()).isEqualTo(department2.getId());
	}

}
