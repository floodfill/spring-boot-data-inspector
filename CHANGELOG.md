# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Scala Implementation** - Complete Scala version of Data Inspector (data-inspector-scala module)
  - Functional Scala with case classes and pattern matching
  - Same features as Java version with Scala idioms
  - Full Spring Boot integration and Java interoperability
  - Comprehensive Scala-specific documentation
- **Export Capabilities** - Export data to CSV, JSON, Excel, HTML, Markdown formats
- **Telemetry & Analytics** - Track usage patterns, popular data sources, and errors
- **License Headers** - MIT license headers on all source files
- JPA/SQL data source provider for database table inspection
- JVM metrics provider (memory, threads, GC, CPU, system info)
- HTTP request tracker with timing and statistics
- Scheduled tasks monitoring (@Scheduled tasks)
- Environment and application properties provider
- Automatic sensitive data masking (passwords, secrets, tokens)
- MongoDB query filtering support
- CustomDataSourceRegistry Map support (registerMap, registerMapSupplier, registerMap with transformers)
- Comprehensive configuration properties with feature toggles
- HTTP request filter auto-registration
- CI/CD GitHub Actions workflow
- Issue templates for bugs and feature requests
- Contributing guidelines
- MIT License
- Export and Analytics documentation (EXPORT_AND_ANALYTICS.md)

### Changed
- Complete README rewrite with comprehensive usage guide
- Enhanced demo application with multiple examples
- Improved project structure and documentation
- All Java files now include proper license headers

### Security
- Added sensitive data masking for environment variables and properties
- Implemented security configuration properties
- Export endpoints support authorization controls

## [1.0.0] - TBD

### Added
- Initial release
- Spring Cache data source provider
- Spring Bean data source provider
- MongoDB data source provider
- Custom data source registry
- REST API for data inspection
- Web UI dashboard
- Auto-configuration support
- Zero-config setup

[Unreleased]: https://github.com/floodfill/spring-boot-data-inspector/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/floodfill/spring-boot-data-inspector/releases/tag/v1.0.0
