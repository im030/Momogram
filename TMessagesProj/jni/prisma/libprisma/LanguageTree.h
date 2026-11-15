#pragma once

#include <atomic>
#include <sstream>
#include <fstream>
#include <mutex>
#include <optional>

#include "Highlight.h"

struct Buffer {
    const std::string &content;
    int64_t offset = 0;
};

class LanguageTree : public std::enable_shared_from_this<LanguageTree>
{
public:
    LanguageTree() = default;

    void load(const std::string& content);

    const Pattern* resolvePattern(size_t path);
    const Grammar* resolveGrammar(size_t path);

    std::map<std::string, std::string> keys() const
    {
        std::map<std::string, std::string> keys;

        for (const auto& kv : m_languages)
        {
            if (kv.second.first.empty())
            {
                continue;
            }

            keys.emplace(kv.first, kv.second.first);
        }

        return keys;
    }

    // The languages as the file lists them: the generator writes each language's own name
    // before its aliases, which is the only place that tells the two apart.
    const std::vector<std::string>& order() const
    {
        return m_order;
    }

    // The display name and the grammar of a name from order().
    const std::pair<std::string, size_t>& language(const std::string& name) const
    {
        return m_languages.at(name);
    }

    std::string languageName(const std::string& language) const
    {
        // A name with no title, as a file extension, reads as itself, as an unknown one does.
        const auto& find = m_languages.find(language);
        if (find != m_languages.end() && !find->second.first.empty())
        {
            return find->second.first;
        }

        return language;
    }

    const Grammar* find(const std::string& key) const
    {
        const auto& value = m_languages.find(key);
        if (value != m_languages.end())
        {
            return m_grammars[value->second.second].get();
        }

        return nullptr;
    }

private:
    void parseLanguages(Buffer &buffer);
    void parseGrammars(Buffer &buffer);
    void parsePatterns(Buffer &buffer);

    std::map<std::string, std::pair<std::string, size_t>> m_languages;
    std::vector<std::string> m_order;
    std::vector<std::shared_ptr<Grammar>> m_grammars;
    // Patterns compile on first use, which can be on any thread. A slot of m_patterns and
    // m_patternsRaw is only touched under m_resolveLock; m_resolved is read without it.
    std::vector<std::shared_ptr<Pattern>> m_patterns;
    std::vector<std::shared_ptr<PatternRaw>> m_patternsRaw;
    std::vector<std::atomic<const Pattern*>> m_resolved;
    std::mutex m_resolveLock;
};