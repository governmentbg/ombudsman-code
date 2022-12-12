<?php

namespace App\Nova\Metrics;

use App\Models\MArticle;
use Laravel\Nova\Http\Requests\NovaRequest;
use Laravel\Nova\Metrics\Partition;

class ArticleperType extends Partition
{
    /**
     * Calculate the value of the metric.
     *
     * @param  \Laravel\Nova\Http\Requests\NovaRequest  $request
     * @return mixed
     */
    public function calculate(NovaRequest $request)
    {
        return $this->count($request, MArticle::join('m_article as m1', 'm1.Ar_Parent_id', '=', 'm_article.Ar_id'), 'm_article.Ar_name');
    }

    public function name()
    {
        return 'Основни секции ';
    }

    /**
     * Determine for how many minutes the metric should be cached.
     *
     * @return  \DateTimeInterface|\DateInterval|float|int
     */
    public function cacheFor()
    {
        return now()->addMinutes(5);
    }

    /**
     * Get the URI key for the metric.
     *
     * @return string
     */
    public function uriKey()
    {
        return 'articleper-type';
    }
}
