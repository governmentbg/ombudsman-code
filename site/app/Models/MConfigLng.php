<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

/**
 * @property int    $CfL_id
 * @property int    $Cf_id
 * @property int    $S_Lng_id
 * @property int    $created_at
 * @property int    $updated_at
 * @property int    $deleted_at
 * @property string $CfL_name
 * @property string $CfL_value
 */
class MConfigLng extends Model
{
    use SoftDeletes;
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_config_lng';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'CfL_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'Cf_id', 'S_Lng_id', 'CfL_name', 'CfL_value', 'created_at', 'updated_at', 'deleted_at'
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
        'CfL_id' => 'int', 'Cf_id' => 'int', 'S_Lng_id' => 'int', 'CfL_name' => 'string', 'CfL_value' => 'string', 'created_at' => 'timestamp', 'updated_at' => 'timestamp', 'deleted_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'created_at', 'updated_at', 'deleted_at'
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

    public function eq_lng()
    {
        return $this->belongsTo(SLang::class, 'S_Lng_id');
    }

    public function eq_config()
    {
        return $this->belongsTo(MConfig::class, 'Cf_id');
    }
}
