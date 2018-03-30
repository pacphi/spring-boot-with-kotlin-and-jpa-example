package io.pivotal.cities.web

import org.springframework.stereotype.Controller
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController {

	@GetMapping
	fun index(model: Model): String {
	    model.addAttribute("message", "Hello fellow Spring Framework developers!")
		return "index"
	}
}