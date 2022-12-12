<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int     $Q_id
 * @property int     $created_at
 * @property int     $updated_at
 * @property int     $deleted_at
 * @property string  $Q_name
 * @property string  $Q_desc
 * @property Date    $Q_date_from
 * @property Date    $Q_date_to
 * @property boolean $St_id
 */
class QQuest extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'q_quest';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'Q_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Q_name', 'Q_desc', 'Q_date_from', 'Q_date_to', 'St_id', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'Q_id' => 'int', 'Q_name' => 'string', 'Q_desc' => 'string', 'Q_date_from' => 'date', 'Q_date_to' => 'date', 'St_id' => 'boolean', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'Q_date_from', 'Q_date_to', 'created_at', 'updated_at', 'deleted_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    // Relations ...
    public function eq_topics()
    {
        return $this->hasMany(QTopic::class, 'Q_id');
    }
}
