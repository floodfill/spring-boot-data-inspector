## Contributing to Data Inspector

First off, thank you for considering contributing to Data Inspector! It's people like you that make Data Inspector such a great tool.

### Code of Conduct

This project and everyone participating in it is governed by our Code of Conduct. By participating, you are expected to uphold this code.

### How Can I Contribute?

#### Reporting Bugs

Before creating bug reports, please check the issue list as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible using our bug report template.

#### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please use our feature request template and provide:

- A clear and descriptive title
- A detailed description of the proposed feature
- Examples of how it would be used
- Why this enhancement would be useful

#### Pull Requests

1. Fork the repo and create your branch from `master`
2. If you've added code that should be tested, add tests
3. If you've changed APIs, update the documentation
4. Ensure the test suite passes
5. Make sure your code follows the existing style
6. Write a clear commit message

### Development Setup

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/spring-boot-data-inspector.git
cd spring-boot-data-inspector

# Build the project
./gradlew clean build

# Run tests
./gradlew test

# Run the demo application
cd demo-app
../gradlew bootRun
```

### Coding Standards

- Follow Java naming conventions
- Use meaningful variable and method names
- Write JavaDoc for public APIs
- Keep methods small and focused
- Write unit tests for new functionality
- Maintain backward compatibility when possible

### Code Style

This project uses:
- 4 spaces for indentation (no tabs)
- 120 character line length
- K&R style braces

### Testing

- Write unit tests for all new functionality
- Maintain or improve code coverage
- Test with multiple Spring Boot versions
- Test with different databases when applicable

### Commit Messages

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests after the first line

Example:
```
Add JPA query filtering support

- Implement filter parsing in JpaDataSourceProvider
- Add filter validation
- Update documentation

Fixes #123
```

### Documentation

- Update README.md with any user-facing changes
- Add JavaDoc for public APIs
- Update configuration examples if adding new properties
- Add to CHANGELOG.md following Keep a Changelog format

### Adding a New Data Source Provider

To add a new data source provider:

1. Create a new class implementing `DataSourceProvider` interface
2. Add `@Component` annotation
3. Implement all required methods
4. Add configuration property (optional)
5. Add tests
6. Add documentation
7. Add example to demo-app

Example:
```java
@Slf4j
@Component
@ConditionalOnClass(YourTechnology.class)
public class YourDataSourceProvider implements DataSourceProvider {

    @Override
    public List<DataSourceInfo> discoverDataSources() {
        // Implementation
    }

    @Override
    public QueryResult query(String dataSourceId, Map<String, Object> filters,
                            int limit, int offset) {
        // Implementation
    }

    @Override
    public boolean supports(String dataSourceId) {
        return dataSourceId != null && dataSourceId.startsWith("your:");
    }
}
```

### Review Process

1. Create a pull request with a clear description
2. Wait for CI checks to pass
3. A maintainer will review your code
4. Address any feedback
5. Once approved, a maintainer will merge

### Release Process

For maintainers:

1. Update version in `build.gradle`
2. Update `CHANGELOG.md`
3. Create a tag: `git tag -a v1.x.x -m "Release v1.x.x"`
4. Push tag: `git push origin v1.x.x`
5. CI will automatically build and release

### Getting Help

- 💬 Join our [Discord/Slack community](link)
- 📖 Read the [documentation](link)
- 🐛 Search [existing issues](https://github.com/floodfill/spring-boot-data-inspector/issues)
- ❓ Ask on [Stack Overflow](https://stackoverflow.com/questions/tagged/data-inspector)

### License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing! 🎉
