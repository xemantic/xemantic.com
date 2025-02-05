module Jekyll
  class SectionWrapper < Converter
    safe true
    priority :low

    def initialize(config = {})
      super(config)
      puts "Section Wrapper plugin initialized!"  # Debug line
    end

    def matches(ext)
      ext =~ /^(.markdown|.md)$/i
    end

    def output_ext(ext)
      ".html"
    end

    def convert(content)
      content = Jekyll::Converters::Markdown.new(@config).convert(content)
      wrapped = content.gsub(/(<h2.*?<\/h2>)(.*?)(?=<h2|\z)/m, '<section>\1\2</section>')
      wrapped
    end
  end
end