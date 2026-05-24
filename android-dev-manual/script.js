(function () {
  const body = document.body;
  const sidebar = document.getElementById("sidebar");
  const menuToggle = document.getElementById("menuToggle");
  const themeToggle = document.getElementById("themeToggle");
  const sectionSearch = document.getElementById("sectionSearch");
  const backToTop = document.getElementById("backToTop");
  const chapters = Array.from(document.querySelectorAll(".chapter"));
  const tocLinks = Array.from(document.querySelectorAll(".toc a"));

  const savedTheme = localStorage.getItem("manual-theme");
  if (savedTheme === "dark") {
    body.classList.add("dark");
    themeToggle.textContent = "淺色";
  }

  menuToggle.addEventListener("click", () => {
    body.classList.toggle("sidebar-open");
  });

  sidebar.addEventListener("click", (event) => {
    if (event.target.matches("a")) {
      body.classList.remove("sidebar-open");
    }
  });

  themeToggle.addEventListener("click", () => {
    body.classList.toggle("dark");
    const isDark = body.classList.contains("dark");
    localStorage.setItem("manual-theme", isDark ? "dark" : "light");
    themeToggle.textContent = isDark ? "淺色" : "深色";
  });

  function normalize(text) {
    return text.toLowerCase().trim();
  }

  sectionSearch.addEventListener("input", () => {
    const query = normalize(sectionSearch.value);
    chapters.forEach((chapter) => {
      const haystack = normalize(`${chapter.textContent} ${chapter.dataset.keywords || ""}`);
      chapter.classList.toggle("hidden", query.length > 0 && !haystack.includes(query));
    });
  });

  const observer = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)[0];

      if (!visible) {
        return;
      }

      const activeId = visible.target.id;
      tocLinks.forEach((link) => {
        link.classList.toggle("active", link.getAttribute("href") === `#${activeId}`);
      });
    },
    {
      rootMargin: "-20% 0px -70% 0px",
      threshold: 0.01
    }
  );

  chapters.forEach((chapter) => observer.observe(chapter));

  document.querySelectorAll("pre").forEach((pre) => {
    const button = document.createElement("button");
    button.className = "copy-button";
    button.type = "button";
    button.textContent = "複製";
    button.addEventListener("click", async () => {
      const text = pre.innerText.replace(/複製$/, "").trimEnd();
      try {
        await navigator.clipboard.writeText(text);
        button.textContent = "已複製";
        setTimeout(() => {
          button.textContent = "複製";
        }, 1400);
      } catch (error) {
        button.textContent = "失敗";
        setTimeout(() => {
          button.textContent = "複製";
        }, 1400);
      }
    });
    pre.appendChild(button);
  });

  window.addEventListener("scroll", () => {
    backToTop.classList.toggle("visible", window.scrollY > 600);
  });

  backToTop.addEventListener("click", () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  });
})();
