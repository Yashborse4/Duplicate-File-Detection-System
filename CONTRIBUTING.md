# Contributing to Duplicate File Detection System (DDAS)

Thank you for your interest in contributing to DDAS! We welcome contributions from the community to help make this project better.

## 🤝 How to Contribute

### Reporting Issues

1. **Search existing issues** first to avoid duplicates
2. **Use the issue templates** when available
3. **Provide detailed information** including:
   - Operating system and version
   - Java version
   - Steps to reproduce the issue
   - Expected vs actual behavior
   - Relevant logs or error messages

### Suggesting Features

1. **Check existing feature requests** to avoid duplicates
2. **Open a discussion** first for major features
3. **Provide clear use cases** and justification
4. **Consider implementation complexity** and maintenance burden

### Code Contributions

1. **Fork the repository** and create a feature branch
2. **Follow coding standards** (see below)
3. **Add tests** for new functionality
4. **Update documentation** as needed
5. **Submit a pull request** with clear description

## 🔧 Development Setup

### Prerequisites

- Java 21 or higher
- MySQL 8.0 or higher
- Git
- IDE of your choice (IntelliJ IDEA recommended)

### Local Environment

1. **Clone your fork**:
   ```bash
   git clone https://github.com/your-username/Duplicate-File-Detection-System.git
   cd Duplicate-File-Detection-System
   ```

2. **Set up database**:
   ```sql
   CREATE DATABASE DDASDB_DEV CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Configure development properties**:
   Create `application-dev.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/DDASDB_DEV
   spring.datasource.username=root
   spring.datasource.password=yourpassword
   ddas.monitored-paths=./test-data
   ddas.duplicate-detection.auto-delete=false
   ```

4. **Run tests**:
   ```bash
   ./gradlew test
   ```

5. **Start development server**:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
   ```

## 📝 Coding Standards

### Java Code Style

- **Follow Oracle Java conventions**
- **Use meaningful variable and method names**
- **Keep methods small and focused** (max 50 lines)
- **Add JavaDoc comments** for public methods and classes
- **Use `@Override` annotations** where applicable

### Example Code Structure

```java
/**
 * Service for handling duplicate file detection operations.
 * 
 * @author Your Name
 * @since 1.0.0
 */
@Service
public class ExampleService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExampleService.class);
    
    @Autowired
    private SomeRepository repository;
    
    /**
     * Processes files for duplicate detection.
     * 
     * @param files List of files to process
     * @return Processing result with statistics
     * @throws ProcessingException if processing fails
     */
    public ProcessingResult processFiles(List<FileMetadata> files) {
        // Implementation here
    }
}
```

### Package Structure

- `controller/` - REST API controllers
- `service/` - Business logic services
- `model/` - JPA entities and DTOs
- `repository/` - Data access layer
- `config/` - Configuration classes
- `util/` - Utility classes
- `exception/` - Custom exceptions

## 🧪 Testing Guidelines

### Test Categories

1. **Unit Tests**: Test individual components in isolation
2. **Integration Tests**: Test component interactions
3. **API Tests**: Test REST endpoints
4. **Performance Tests**: Test system performance

### Test Structure

```java
@ExtendWith(SpringExtension.class)
@SpringBootTest
class ExampleServiceTest {
    
    @MockBean
    private SomeRepository mockRepository;
    
    @Autowired
    private ExampleService service;
    
    @Test
    @DisplayName("Should process files successfully")
    void shouldProcessFilesSuccessfully() {
        // Given
        List<FileMetadata> testFiles = createTestFiles();
        
        // When
        ProcessingResult result = service.processFiles(testFiles);
        
        // Then
        assertThat(result.getProcessedCount()).isEqualTo(testFiles.size());
    }
}
```

### Test Data

- Use **test-specific data** that doesn't affect other tests
- **Clean up** test data after tests complete
- Use **meaningful test names** that describe the scenario

## 📚 Documentation

### Code Documentation

- **JavaDoc** for all public classes and methods
- **Inline comments** for complex logic
- **README updates** for new features
- **API documentation** for new endpoints

### Commit Messages

Use conventional commit format:

```
type(scope): brief description

Longer description if needed

Fixes #123
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (no logic change)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
- `feat(api): add endpoint for bulk file processing`
- `fix(scanner): handle symbolic link detection correctly`
- `docs(readme): update installation instructions`

## 🚀 Pull Request Process

### Before Submitting

1. **Run all tests** and ensure they pass
2. **Update documentation** if needed
3. **Test your changes** manually
4. **Rebase** your branch on the latest main
5. **Squash commits** if you have multiple small commits

### PR Description Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No breaking changes (or marked as such)
```

### Review Process

1. **Automated checks** must pass (CI/CD)
2. **Code review** by maintainers
3. **Testing** in development environment
4. **Approval** required before merge

## 🐛 Bug Fix Guidelines

### Priority Levels

- **Critical**: System crashes, data loss, security issues
- **High**: Major functionality broken
- **Medium**: Minor functionality issues
- **Low**: Cosmetic or enhancement requests

### Bug Fix Process

1. **Reproduce the issue** locally
2. **Write a failing test** that captures the bug
3. **Fix the issue** with minimal changes
4. **Verify the test passes** and no regressions
5. **Update documentation** if needed

## 🌟 Feature Development

### Feature Request Process

1. **Open a discussion** for large features
2. **Get approval** from maintainers
3. **Create detailed design** if complex
4. **Break down** into smaller tasks
5. **Implement incrementally** with tests

### Feature Guidelines

- **Maintain backward compatibility** when possible
- **Add configuration options** for new behavior
- **Include comprehensive tests**
- **Update API documentation**
- **Consider performance impact**

## 📋 Code Review Guidelines

### For Authors

- **Keep PRs small** and focused
- **Provide context** in description
- **Respond promptly** to feedback
- **Test thoroughly** before requesting review

### For Reviewers

- **Be constructive** and respectful
- **Focus on code quality** and maintainability
- **Check for edge cases** and error handling
- **Verify tests** are adequate

## 🏆 Recognition

Contributors will be recognized in:
- **README.md** contributor section
- **Release notes** for significant contributions
- **GitHub contributors** page

## 📞 Getting Help

- **GitHub Discussions** for general questions
- **GitHub Issues** for bugs and feature requests
- **Email** maintainers for sensitive issues

## 📄 License Agreement

By contributing to this project, you agree that your contributions will be licensed under the same MIT License that covers the project.
