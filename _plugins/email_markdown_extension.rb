module Jekyll
  class EmailMarkdownExtension
    def initialize(config)
      Jekyll.logger.info "EmailMarkdown:", "Initializing email extension"
    end

    def process(content)
      content.gsub(/\<([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\>/) do |match|
        email = $1.strip
        encoded_email = email.chars.map { |c| "%" + c.ord.to_s(16) }.join
        id = "email-#{rand(1000000)}"

        "<a href=\"mailto:#{encoded_email}\" id=\"#{id}\">loading...</a><script>(function(){const link=document.getElementById(\"#{id}\");link.textContent=decodeURIComponent(\"#{encoded_email}\");})();</script>"
      end
    end
  end
end

Jekyll::Hooks.register [:pages, :documents], :pre_render do |doc|
  if doc.extname == '.md'
    extension = Jekyll::EmailMarkdownExtension.new(doc.site.config)
    doc.content = extension.process(doc.content)
  end
end