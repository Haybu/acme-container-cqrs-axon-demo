/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.agilehandy.query.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author Haytham Mohamed
 **/
@Embeddable
@Data
@AllArgsConstructor
public class InventoryKey implements Serializable {

	@NotNull
	private String zoneName;

	@NotNull
	private String portName;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InventoryKey that = (InventoryKey) o;
		return this.zoneName.equals(that.zoneName) && this.portName.equals(that.portName) ;
	}

	@Override
	public int hashCode() {
		int result = zoneName.hashCode() + portName.hashCode();
		result = 31 * result + 32 * zoneName.hashCode() + 33 * portName.hashCode();
		return result;
	}
}
